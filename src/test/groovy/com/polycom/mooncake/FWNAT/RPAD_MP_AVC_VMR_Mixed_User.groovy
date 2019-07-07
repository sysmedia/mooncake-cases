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
 * Created by dyuan on 6/4/2019
 *
 * # Test the AVC VMR by mixed call method:
 * # 1. H323 guest user
 * # 2. SIP guest user
 * # 3. SIPTLS registered user
 * # 4. H323 registered user
 * # Description: RMX1800, RPAD environment: SIP&H323, AVC VMR, Int&Ext, Privisoned&Registered&Unregistered
 */
class RPAD_MP_AVC_VMR_Mixed_User extends MoonCakeSystemTestSpec {
    @Shared
    RpdWin rpdWin

    @Shared
    Dma dma

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

        //Create AVC only conference template
        dma.createConferenceTemplate(confTmpl, "AVC only template", "2048", ConferenceCodecSupport.AVC)

        mc_alias = generateDialString(moonCake)
        rpd_alias = generateDialString(rpdWin)

        rpdWin = testContext.bookSut(RpdWin.class, "FWNAT")
        rpdWin.init()
        rpdWin.enableSip()
        rpdWin.registersip(dma.ip, rpd_alias.sipUri, "polycom.com", "", "", "TCP")
    }

    def cleanupSpec() {
        dma.deleteConferenceTemplateByName(confTmpl)

        testContext.releaseSut(dma)
        testContext.releaseSut(rpdWin)
    }

    @Unroll
    def "Verify mooncake can join conference with guest #calltype and #pool_order"() {
        setup:
        //Create VMR on DMA
        dma.createVmr(vmr, confTmpl, pool_order, dma.domain, dma.username, confPwd, null)
        if (calltype == CallType.SIP) {
            moonCake.enableSIP()
            moonCake.registerSip("", false, "", "", "", "", "")
        } else {
            moonCake.enableH323()
            moonCake.registerGk(false, false, "", "", "", "", "")
        }
        pauseTest(5)


        when: "Rpdwin call in"
        rpdWin.placeCall(vmr, CallType.SIP, 1920)
        pauseTest(2)
        rpdWin.sendDtmf(confPwd + "#")
        pauseTest(5)
        rpdWin.pushContent()
        pauseTest(5)


        then: "Verify the rpdwin's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:0:--", rpdWin)


        when: "Mooncake call in"
        moonCake.placeCall(vmr + "@" + rpad_ip, calltype, 2048)
        pauseTest(2)
        moonCake.sendDTMF(confPwd + "#")
        pauseTest(10)

        then: "Verify the Rpdwin's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:0:--")

        when: "Mooncake push content"
        moonCake.pushContent()
        pauseTest(10)

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


        cleanup:
        moonCake.hangUp()
        rpdWin.hangUp()
        dma.deleteVmr(vmr)
        pauseTest(10)

        where:
        calltype      | pool_order               | vmr
        CallType.SIP  | "RMX1800_10.220.206.102" | "6893"
        CallType.H323 | "RMX1800_10.220.206.102" | "6894"
        CallType.SIP  | "RMX2000_172.21.113.222" | "6895"
        CallType.H323 | "RMX2000_172.21.113.222" | "6896"
    }

    @Unroll
    def "Verify mooncake can join conference with #calltype register and #pool_order"() {
        setup:
        //Create VMR on DMA
        dma.createVmr(vmr, confTmpl, pool_order, dma.domain, dma.username, confPwd, null)
        if (calltype == CallType.SIP) {
            moonCake.registerSip("TLS", true, "polycom.com", rpad_ip, "", mc_alias.sipUri, "")
        } else {
            moonCake.registerGk(true, false, rpad_ip, mc_alias.h323Name, mc_alias.e164Number, "", "")
        }
        pauseTest(5)


        when: "Rpdwin call in"
        rpdWin.placeCall(vmr, CallType.SIP, 1920)
        pauseTest(2)
        rpdWin.sendDtmf(confPwd + "#")
        pauseTest(5)
        rpdWin.pushContent()
        pauseTest(5)


        then: "Verify the rpdwin's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:0:--", rpdWin)


        when: "Mooncake call in"
        moonCake.placeCall(vmr, calltype, 2048)
        pauseTest(2)
        moonCake.sendDTMF(confPwd + "#")
        pauseTest(10)

        then: "Verify the Rpdwin's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:0:--")

        when: "Mooncake push content"
        moonCake.pushContent()
        pauseTest(10)

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


        cleanup:
        moonCake.hangUp()
        rpdWin.hangUp()
        dma.deleteVmr(vmr)
        pauseTest(10)

        where:
        calltype      | pool_order               | vmr
        CallType.SIP  | "RMX1800_10.220.206.102" | "6893"
        CallType.H323 | "RMX1800_10.220.206.102" | "6894"
        CallType.SIP  | "RMX2000_172.21.113.222" | "6895"
        CallType.H323 | "RMX2000_172.21.113.222" | "6896"
    }
}
