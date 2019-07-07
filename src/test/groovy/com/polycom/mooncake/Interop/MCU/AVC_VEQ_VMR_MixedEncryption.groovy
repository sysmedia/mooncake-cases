package com.polycom.mooncake.Interop.MCU

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.api.rest.plcm_conference_template_v7.EncryptionPolicy
import com.polycom.auto.resources.enums.SipTransportProtocol
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.Mcu
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Gary Wang on 6/19/2019
 */
class AVC_VEQ_VMR_MixedEncryption extends MoonCakeSystemTestSpec {
    @Shared
    GroupSeries groupSeries

    @Shared
    Dma dma

    @Shared
    Mcu mcu

    @Shared
    String moonsipUri

    @Shared
    String gsSipUserName = "gsSipTest"

    @Shared
    String  password = "123456"

    def setupSpec() {
        moonCake.enableSIP()
        groupSeries = testContext.bookSut(GroupSeries.class, "GS700")
        groupSeries.init()
        groupSeries.enableSIP()
        groupSeries.setEncryption("yes")

        dma = testContext.bookSut(Dma.class, keyword)
        try {
            dma.deleteVeq(veqAuto)
        } catch (Exception e) {
            logger.info("delete veq auto error")
        }
        try {
            dma.deleteVeq(veqOff)
        } catch (Exception e) {
            logger.info("delete veq off error")
        }
        try {
            dma.deleteVmr(vmrAVCAuto)
        } catch (Exception e) {
            logger.info("delete vmr auto error")
        }
        try {
            dma.deleteVmr(vmrAVCOff)
        } catch (Exception e) {
            logger.info("delete vmr off error")
        }
        try {
            dma.deleteConferenceTemplateByName(callTmplAVCAuto)
        } catch (Exception e) {
            logger.info("delete template auto error")
        }
        try {
            dma.deleteConferenceTemplateByName(callTmplAVCOff)
        } catch (Exception e) {
            logger.info("delete template off error")
        }


        dma.createVeq(veqAuto, avcAutoEqName)
        dma.createVeq(veqOff, avcOffEqName)
        dma.createConferenceTemplate(callTmplAVCAuto, "AVC only call template AES Auto", "1024", ConferenceCodecSupport.AVC, EncryptionPolicy.ENCRYPTED_PREFERRED)
        dma.createConferenceTemplate(callTmplAVCOff, "AVC only call template AES Off", "1024", ConferenceCodecSupport.AVC)
        dma.createVmr(vmrAVCAuto, callTmplAVCAuto, poolOrder, dma.domain, dma.username, password, null)
        dma.createVmr(vmrAVCOff, callTmplAVCOff, poolOrder, dma.domain, dma.username, password, null)

        moonsipUri = generateDialString(moonCake).sipUri
        moonCake.updateCallSettings(1024, "auto", true, false, true)
        moonCake.registerSip("TLS", true, "", dma.ip, moonsipUri, moonsipUri, "")
        gsSipUserName = generateDialString(groupSeries).sipUri
        groupSeries.registerSip(gsSipUserName, centralDomain, "",dma.ip,SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TLS)

    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        dma.deleteVmr(vmrAVCAuto)
        dma.deleteVmr(vmrAVCOff)
        dma.deleteConferenceTemplateByName(callTmplAVCAuto)
        dma.deleteConferenceTemplateByName(callTmplAVCOff)
        dma.deleteVeq(veqAuto)
        dma.deleteVeq(veqOff)
        testContext.releaseSut(dma)
       // moonCake.registerSip("TCP", false, "", "", "", "", "")
    }

    @Unroll
    def "MoonCake join conference via VEQ + VMR, Group join conference via VMR" (String veq, String vmr) {
        setup: "make sure endpoints was not in call"

        moonCake.hangUp()
        groupSeries.hangUp()

        when: "moonCake place VEQ call, then switch to Vmr"
        logger.info("===============Start SIP Call===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(veq, CallType.SIP, 1024)
            pauseTest(3)
            moonCake.sendDTMF(vmr + "#")
            pauseTest(3)
            moonCake.sendDTMF(password + "#")
            pauseTest(3)
        }

        then: "GS place call"
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(vmr, CallType.SIP, 1024)
            pauseTest(3)
            groupSeries.sendDtmf(password + "#")
            pauseTest(2)
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        then: "GS push content"
        groupSeries.playHdmiContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--")

        then: "moonCake push content"
        pauseTest(10)
        retry(times: 3, delay: 5) {
            moonCake.pushContent()
        }
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")

        cleanup: "hang up all endpoints"
        groupSeries.hangUp()
        moonCake.hangUp()
        pauseTest(20)

        where:
        [veq, vmr] << getTestData_1()
    }

    def getTestData_1() {
        def rtn = []
        rtn << [veqAuto, vmrAVCAuto]
        rtn << [veqAuto, vmrAVCOff]
        rtn << [veqOff, vmrAVCAuto]
        rtn << [veqOff, vmrAVCOff]
        return rtn
    }
}
