package com.polycom.mooncake.Interop.MCU

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.auto.resources.enums.SipTransportProtocol
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.RpdWin
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared

/**
 * Created by qxu on 5/6/2019
 */
class AVC_VEQ_Mixed_VMR extends MoonCakeSystemTestSpec {
    @Shared
    GroupSeries groupSeries

    @Shared
    Dma dma

    @Shared
    RpdWin rpdWin

    @Shared
    String moonsipUri

    @Shared
    String gsSipUserName

    @Shared
    String rpdH323UsrName

    @Shared
    String rpdExtNum

    @Shared
    String vmrMixed = "171819"

    @Shared
    String veq = "171010"

    @Shared
    String callTmplMix = "mixtmpltest"

    @Shared
    String avcEqName = "automation_CP_Only_EQ"

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        rpdWin = testContext.bookSut(RpdWin.class, keyword)
        dma = testContext.bookSut(Dma.class, keyword)

        groupSeries.init()
        groupSeries.enableSIP()
        groupSeries.setEncryption("no")
        rpdWin.enableH323()
        moonCake.enableSIP()
        moonCake.setEncryption("off")

        //create VEQ
        dma.createVeq(veq, avcEqName)
        //create Mixed template
        dma.createConferenceTemplate(callTmplMix, "mixed call template", "2048", ConferenceCodecSupport.MIXED)
        //Create VMR on DMA
        dma.createVmr(vmrMixed, callTmplMix, poolOrder, dma.domain, dma.username, null, null)
        moonsipUri = generateDialString(moonCake).sipUri
        gsSipUserName = generateDialString(groupSeries).sipUri
        rpdH323UsrName = generateDialString(rpdWin).h323Name
        rpdExtNum = generateDialString(rpdWin).e164Number
    }

    def cleanupSpec() {
        if (groupSeries != null) {
            testContext.releaseSut(groupSeries)
        }
        if (rpdWin != null) {
            testContext.releaseSut(rpdWin)
        }
        //Delete VMR
        dma.deleteVmr(vmrMixed)
        dma.deleteConferenceTemplateByName(callTmplMix)
        dma.deleteVeq(veq)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    def "SUT and endpoints join conference form AVC VEQ + Mixed VMR"() {
        setup: "make sure endpoints was not in call"
        hangUpAll(moonCake, groupSeries, rpdWin)

        when: "register endpoints"
        endpointsRegister()

        then: "moonCake place VEQ call, then switch to Vmr"
        logger.info("===============Start SIP Call===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(veq, CallType.SIP, 1024)
            pauseTest(2)
            moonCake.sendDTMF(vmrMixed + "#")
            pauseTest(2)
            moonCake.sendDTMF(vmrMixed + "#")
        }
        pauseTest(5)

        then: "GS place call"
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(veq, CallType.SIP, 2048)
            groupSeries.sendDtmf(vmrMixed + "#")
            pauseTest(2)
            groupSeries.sendDtmf(vmrMixed + "#")
        }
        pauseTest(5)

        then: "Rpd place H323 call"
        retry(times: 3, delay: 5) {
            rpdWin.placeCall(vmrMixed, CallType.H323, 1920)
        }
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        then: "Rpd push content"
        retry(times: 3, delay: 5) {
            rpdWin.pushContent()
        }
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--")

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
        hangUpAll(moonCake, groupSeries, rpdWin)
    }

    def endpointsRegister() {
        moonCake.registerSip("TCP", true, "", dma.ip, moonsipUri, moonsipUri, "")
        groupSeries.registerSip(gsSipUserName, centralDomain, "", dma.ip, SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TCP)
        rpdWin.registerH323(dma.ip, rpdH323UsrName, rpdExtNum)
    }
}
