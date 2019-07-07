package com.polycom.mooncake.FWNAT

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.RpdWin
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by dyuan on 6/1/2019
 */
class RPAD_MP_AVC_VEQ_RMX extends MoonCakeSystemTestSpec {
    @Shared
    RpdWin rpdWin

    @Shared
    Dma dma

    @Shared
    String vmr = "7788"

    @Shared
    String confPwd = "1234"

    @Shared
    def mc_alias

    @Shared
    def rpd_alias

    def setupSpec() {
        rpad_ip = testContext.getValue("Rpad_ip")

        dma = testContext.bookSut(Dma.class, "FWNAT")

        //Create AVC only conference template
        dma.createConferenceTemplate(confTmpl, "AVC only template", "2048", ConferenceCodecSupport.AVC)

        mc_alias = generateDialString(moonCake)
        rpd_alias = generateDialString(rpdWin)

        rpdWin = testContext.bookSut(RpdWin.class, "FWNAT")
        rpdWin.enableSip()
        rpdWin.registersip(dma.ip, rpd_alias.sipUri, "polycom.com", "", "", "TLS")
    }

    def cleanupSpec() {
        dma.deleteConferenceTemplateByName(confTmpl)

        testContext.releaseSut(dma)
        testContext.releaseSut(rpdWin)
    }

    @Unroll
    def "Verify mooncake can join veq conference and show content with SIP and #pool_order"() {
        setup:
        moonCake.hangUp()
        rpdWin.hangUp()

        moonCake.enableSIP()
        moonCake.setEncryption("auto")

        //Create VMR on DMA
        dma.createVmr(vmr, confTmpl, pool_order, dma.domain, dma.username, confPwd, null)

        moonCake.registerSip("TLS", true, "polycom.com", rpad_ip, "", mc_alias.sipUri, "")

        pauseTest(5)


        when: "Mooncake call in"
        moonCake.placeCall(avc_eq, CallType.SIP, 1024)
        pauseTest(2)
        moonCake.sendDTMF(vmr + "#")
        pauseTest(2)
        moonCake.sendDTMF(confPwd + "#")
        pauseTest(5)

        then: "Verify the MoonCake's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")


        when: "Rpdwin call in"
        rpdWin.placeCall(vmr, CallType.SIP, 1024)
        pauseTest(2)
        rpdWin.sendDtmf(confPwd + "#")
        pauseTest(5)

        then: "Verify the Rpdwin's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdWin)

        when: "Mooncake push content"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the MoonCake's media statistics during push content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:0:--", rpdWin)

        when: "Rpdwin push content"
        rpdWin.pushContent()
        pauseTest(5)

        then: "Verify the MoonCake's media statistics during push content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.CVTX, "--:--:--:--:0:--", rpdWin)

        cleanup:
        dma.deleteVmr(vmr)
        moonCake.hangUp()
        rpdWin.hangUp()
        pauseTest(5)

        where:
        pool_order               | avc_eq
        "RMX1800_10.220.206.102" | "3114"
        "RMX2000_172.21.113.222" | "3322"
    }

    @Unroll
    def "Verify mooncake can not join veq conference with H323 and #pool_order"() {
        setup:
        moonCake.hangUp()

        moonCake.enableH323()
        moonCake.registerGk(true, false, rpad_ip, mc_alias.h323Name, mc_alias.e164Number, "", "")

        pauseTest(5)


        when: "Mooncake call in"
        moonCake.placeCall(avc_eq, CallType.H323, 1024)


        then: "Verify could not call in"
        moonCake.callStatus == "IDLE"

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        pool_order               | avc_eq
        "RMX1800_10.220.206.102" | "3114"
        "RMX2000_172.21.113.222" | "3322"
    }
}
