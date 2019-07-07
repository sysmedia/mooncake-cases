package com.polycom.mooncake.Interop.MCU

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.auto.resources.enums.SipTransportProtocol
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.Mcu
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared

/**
 * Created by qxu on 5/6/2019
 */
class AVC_VEQ_AVC_VMR extends MoonCakeSystemTestSpec {
    @Shared
    GroupSeries groupSeries

    @Shared
    Dma dma

    @Shared
    Mcu mcu

    @Shared
    String moonsipUri

    @Shared
    String vmrAVC = "445566"

    @Shared
    String veq = "440011"

    @Shared
    String gsSipUserName = "gsSipTest"

    @Shared
    String callTmplAVC = "avctmpltest"

    @Shared
    String avcEqName = "automation_CP_Only_EQ"


    def setupSpec() {
        moonCake.enableSIP()
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.enableSIP()
        groupSeries.setEncryption("no")
        dma = testContext.bookSut(Dma.class, keyword)
        dma.createVeq(veq, avcEqName)
        dma.createConferenceTemplate(callTmplAVC, "AVC only call template", "2048", ConferenceCodecSupport.AVC)
        dma.createVmr(vmrAVC, callTmplAVC, poolOrder, dma.domain, dma.username, null, null)
        moonsipUri = generateDialString(moonCake).sipUri
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        dma.deleteVmr(vmrAVC)
        dma.deleteConferenceTemplateByName(callTmplAVC)
        dma.deleteVeq(veq)
        testContext.releaseSut(dma)
        moonCake.registerSip("TCP", false, "", "", "", "", "")
    }

    def "SUT and endpoints join conference form AVC VEQ + AVC VMR"() {
        setup: "make sure endpoints was not in call"
        moonCake.updateCallSettings(1024, "off", true, false, true)
        hangUpAll(moonCake, groupSeries)

        when: "register endpoints"
        endpointsRegister()

        then: "moonCake place VEQ call, then switch to Vmr"
        logger.info("===============Start SIP Call===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(veq, CallType.SIP, 1024)
            pauseTest(2)
            moonCake.sendDTMF(vmrAVC + "#")
            pauseTest(2)
            moonCake.sendDTMF(vmrAVC + "#")
            pauseTest(2)
        }

        then: "GS place call"
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(veq, CallType.SIP, 2048)
            groupSeries.sendDtmf(vmrAVC + "#")
            pauseTest(2)
            groupSeries.sendDtmf(vmrAVC + "#")
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
        hangUpAll(moonCake, groupSeries)
    }

    def endpointsRegister() {
        moonCake.registerSip("TCP", true, "", dma.ip, moonsipUri, moonsipUri, "")
        groupSeries.registerSip(gsSipUserName, centralDomain, "", dma.ip, SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TCP)
        pauseTest(2)
    }
}
