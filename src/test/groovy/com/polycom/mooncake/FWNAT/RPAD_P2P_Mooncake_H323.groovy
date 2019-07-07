package com.polycom.mooncake.FWNAT

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared

/**
 * Created by dyuan on 6/13/2019
 * # External Oculus and internal Oculus P2P H323 call.
 * # 1. H323 guest users
 * # 2. H323 registered users
 * # RPAD environment
 */
class RPAD_P2P_Mooncake_H323 extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    MoonCake mc

    @Shared
    def mc_alias

    @Shared
    def mc_alias1

    def setupSpec() {
        rpad_ip = testContext.getValue("Rpad_ip")
        moonCake.enableH323()
        mc = testContext.bookSut(MoonCake.class, "Dancy")
        mc.init()
        mc.enableH323()
        dma = testContext.bookSut(Dma.class, "FWNAT")
        mc_alias = generateDialString(moonCake)
        mc_alias1 = generateDialString(mc)
        mc.registerGk(true, false, dma.ip, mc_alias1.h323Name, mc_alias1.e164Number, "", "")
    }

    def cleanupSpec() {
        mc.init()
        testContext.releaseSut(mc)
        testContext.releaseSut(dma)
    }

    def "Verify external mooncake guest call internal mooncake"() {
        setup:
        moonCake.registerGk(false, false, "", "", "", "", "")
        pauseTest(5)

        when: "External mooncake call internal moonccake"
        moonCake.placeCall(mc_alias1.h323Name + "@" + rpad_ip, CallType.H323, 2048)
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
        moonCake.sendFECC("RIGHT")
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
        mc.sendFECC("RIGHT")
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
        pauseTest(5)
    }

    def "Verify external mooncake register call internal mooncake"() {
        setup:
        moonCake.registerGk(true, false, rpad_ip, mc_alias.h323Name, mc_alias.e164Number, "", "")
        pauseTest(5)

        when: "External mooncake call internal moonccake"
        moonCake.placeCall(mc_alias1.h323Name, CallType.H323, 2048)
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
        moonCake.sendFECC("RIGHT")
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
        mc.sendFECC("RIGHT")
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
        pauseTest(5)
    }
}
