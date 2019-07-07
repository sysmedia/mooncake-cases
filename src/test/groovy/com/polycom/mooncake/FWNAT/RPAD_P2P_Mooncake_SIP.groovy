package com.polycom.mooncake.FWNAT

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import polycom.serviceapi.camera.ContentState
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by dyuan on 6/13/2019
 * # External Oculus and internal Oculus P2P SIP call.
 * # 1. SIP guest users
 * # 2. SIPTCP registered users
 * # 3. STPUDP registered users
 * # 4. SIPTLS registered users
 */
class RPAD_P2P_Mooncake_SIP extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    MoonCake mc

    @Shared
    String mc_sip

    @Shared
    String mc_sip1

    def setupSpec() {
        rpad_ip = testContext.getValue("Rpad_ip")
        moonCake.enableSIP()
        mc = testContext.bookSut(MoonCake.class, "Dancy")
        mc.init()
        mc.enableSIP()
        dma = testContext.bookSut(Dma.class, "FWNAT")
        mc_sip = generateDialString(moonCake).sipUri
        mc_sip1 = generateDialString(mc).sipUri
    }

    def cleanupSpec() {
        testContext.releaseSut(mc)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    def "verify external mooncake sip guest call internal mooncake"() {
        setup:
        moonCake.setEncryption("auto")
        moonCake.registerSip("", false, "", "", "", "", "")
        mc.setEncryption("auto")
        mc.registerSip("TLS", true, "polycom.com", dma.ip, "", mc_sip1, "")
        pauseTest(5)

        when: "External mooncake call internal moonccake"
        moonCake.placeCall(mc_sip1 + "@" + rpad_ip, CallType.SIP, 2048)
        pauseTest(5)

        then: "Verify the MoonCake's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")

        and: "Verify the mc's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", mc)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", mc)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", mc)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", mc)

        when: "Mooncake push content"
        moonCake.sendFECC("LEFT")
        pauseTest(5)
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the MoonCake's media statistics during push content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", mc)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", mc)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", mc)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", mc)
        verifyMediaStatistics(MediaChannelType.CVRX, "--:--:--:--:0:--", mc)

        when: "mc push content"
        mc.sendFECC("LEFT")
        pauseTest(5)
        mc.pushContent()
        pauseTest(5)

        then: "Verify the MoonCake's media statistics during push content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", mc)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", mc)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", mc)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", mc)
        verifyMediaStatistics(MediaChannelType.CVTX, "--:--:--:--:0:--", mc)

        cleanup:
        mc.hangUp()
        moonCake.hangUp()
        pauseTest(10)
    }

    @Unroll
    def "verify external mooncake sip #protocol register p2p call internal mooncake"() {
        setup:
        moonCake.setEncryption(aes)
        moonCake.registerSip(protocol, true, "polycom.com", rpad_ip, "", mc_sip, "")
        mc.setEncryption(aes)
        mc.registerSip(protocol, true, "polycom.com", dma.ip, "", mc_sip1, "")
        pauseTest(5)

        when: "External mooncake call internal moonccake"
        moonCake.placeCall(mc_sip1, CallType.SIP, 2048)
        pauseTest(5)

        then: "Verify the MoonCake's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")

        and: "Verify the mc's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", mc)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", mc)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", mc)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", mc)

        when: "Mooncake push content"
        moonCake.sendFECC("LEFT")
        pauseTest(5)
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the MoonCake's media statistics during push content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", mc)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", mc)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", mc)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", mc)
        verifyMediaStatistics(MediaChannelType.CVRX, "--:--:--:--:0:--", mc)

        when: "mc push content"
        mc.sendFECC("LEFT")
        pauseTest(5)
        mc.pushContent()
        pauseTest(5)

        then: "Verify the MoonCake's media statistics during push content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", mc)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", mc)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", mc)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", mc)
        verifyMediaStatistics(MediaChannelType.CVTX, "--:--:--:--:0:--", mc)

        cleanup:
        mc.hangUp()
        moonCake.hangUp()
        pauseTest(10)

        where:
        aes    | protocol
        "off"  | "TCP"
        "off"  | "UDP"
        "auto" | "TLS"
    }

}
