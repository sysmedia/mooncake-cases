package com.polycom.mooncake.FWNAT

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.auto.resources.enums.SipTransportProtocol
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.Mcu
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.RpdWin
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by dyuan on 5/29/2019
 *
 * # TLS on and SRTP off, Bridge (MCU register to DMA as SIP proxy) with password
 * # 1. register ACME with siptls in standalone mode
 * # 2. enterprise user
 * # AVC conference rate is 4096
 * #RPAD environment: SIP&H323, AVC Bridge - MCU register to DMA as SIP proxy, Int&Ext, Privisoned&Registered
 */
class RPAD_MP_AVC_Bridge_RMX extends MoonCakeSystemTestSpec {
    @Shared
    RpdWin rpdWin

    @Shared
    Dma dma

    @Shared
    String vmrWithPwd = "3968"

    @Shared
    String confPwd = "1234"

    @Shared
    def mc_alias

    @Shared
    def rpd_alias

    def setupSpec() {
        rpad_ip = testContext.getValue("Rpad_ip")

        moonCake.enableSIP()
        moonCake.setEncryption("auto")

        dma = testContext.bookSut(Dma.class, "FWNAT")

        rpdWin = testContext.bookSut(RpdWin.class, "FWNAT")

        //Create AVC only conference template
        dma.createConferenceTemplate(confTmpl, "AVC only template", "2048", ConferenceCodecSupport.AVC)

        mc_alias = generateDialString(moonCake)
        rpd_alias = generateDialString(rpdWin)

        rpdWin.enableSip()
        rpdWin.registersip(dma.ip, rpd_alias.sipUri, "polycom.com", "", "", "TCP")
    }

    def cleanupSpec() {
        dma.deleteConferenceTemplateByName(confTmpl)

        testContext.releaseSut(dma)
        testContext.releaseSut(rpdWin)
    }

    @Unroll
    def "Verify mooncake can join vmr conference and show content with #calltype and #pool_order"() {
        setup:
        moonCake.hangUp()
        rpdWin.hangUp()

        //Create VMR on DMA
        dma.createVmr(vmrWithPwd, confTmpl, pool_order, dma.domain, dma.username, confPwd, null)
        if (calltype == CallType.SIP) {
            moonCake.registerSip("TLS", true, "polycom.com", rpad_ip, "", mc_alias.sipUri, "")
        } else {
            moonCake.registerGk(true, false, rpad_ip, mc_alias.h323Name, mc_alias.e164Number, "", "")
        }
        pauseTest(5)


        when: "Mooncake call in"
        moonCake.placeCall(vmrWithPwd, calltype, 2048)
        pauseTest(2)
        moonCake.sendDTMF(confPwd + "#")
        pauseTest(5)


        then: "Verify the MoonCake's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")


        when: "Rpdwin call in"
        rpdWin.placeCall(vmrWithPwd, CallType.SIP, 1024)
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
        verifyMediaStatistics(MediaChannelType.CVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.CVRX, "--:--:--:--:0:--", rpdWin)

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
        dma.deleteVmr(vmrWithPwd)
        moonCake.hangUp()
        rpdWin.hangUp()
        pauseTest(5)

        where:
        calltype      | pool_order
        CallType.SIP  | "RMX1800_10.220.206.102"
        CallType.H323 | "RMX1800_10.220.206.102"
        CallType.SIP  | "RMX2000_172.21.113.222"
        CallType.H323 | "RMX2000_172.21.113.222"

    }
}
