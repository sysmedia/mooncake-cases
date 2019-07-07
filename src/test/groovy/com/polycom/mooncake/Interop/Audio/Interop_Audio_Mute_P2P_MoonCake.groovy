package com.polycom.mooncake.Interop.Audio

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Dancy Li on 5/29/2019
 * This case is to verify the call between MoonCake and another MoonCake should work well
 * when MoonCake mutes audio for more than 3 minutes
 * Far end mutes audio for more than 3 minutes
 * Different call type: H323, SIP
 */

class Interop_Audio_Mute_P2P_MoonCake extends MoonCakeSystemTestSpec {
    @Shared
    MoonCake moonCake_1

    @Shared
    Dma dma

    @Shared
    String mc_e164

    @Shared
    String mc_h323Name

    @Shared
    String mc_1_e164

    @Shared
    String mc_1_h323Name

    @Shared
    String sipUri

    @Shared
    String sipUri_1

    def setupSpec() {
        moonCake_1 = testContext.bookSut(MoonCake.class, keyword, "backup")
        moonCake_1.init()
        moonCake_1.setEncryption("no")
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
        def mcDialString = generateDialString(moonCake)
        def mc_1_DialString = generateDialString(moonCake_1)
        mc_e164 = mcDialString.e164Number
        mc_h323Name = mcDialString.h323Name
        mc_1_e164 = mc_1_DialString.e164Number
        mc_1_h323Name = mc_1_DialString.h323Name
        sipUri = mcDialString.sipUri
        sipUri_1 = mc_1_DialString.sipUri
    }

    def cleanupSpec() {
        moonCake.init()
        moonCake_1.init()
        testContext.releaseSut(moonCake_1)
        testContext.releaseSut(dma)
    }

    @Unroll
    def "Verify H323 P2P call when MoonCake call MoonCake_1 and mute audio with call rate 2048 Kbps"() {
        setup:
        moonCake.hangUp()
        moonCake_1.hangUp()

        when: "Set both MoonCake and MoonCake_1 H323 registered"
        moonCake.enableH323()
        moonCake_1.enableH323()
        moonCake.registerGk(true, false, dma.ip, mc_h323Name, mc_e164, "", "")
        moonCake_1.registerGk(true, false, dma.ip, mc_1_h323Name, mc_1_e164, "", "")
        pauseTest(3)

        then: "MoonCake place call to MoonCake_1 with call rate 2048"
        logger.info("===============Start H323 Call with call rate 2048===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(mc_1_e164, CallType.H323, 2048)
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        then: "Mute audio for MoonCake and last 3 minutes"
        moonCake.muteAudio(true)
        pauseTest(200)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        then: "Mute audio for MoonCake_1 and last 3 minutes"
        moonCake_1.muteAudio(true)
        pauseTest(200)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        logger.info("===============Successfully start H.323 Call with call rate 2048===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(2)
    }

    @Unroll
    def "Verify SIP P2P call when MoonCake call MoonCake_1 and mute audio with call rate 4096 Kbps"() {
        setup:
        moonCake.hangUp()
        moonCake_1.hangUp()

        when: "Set both MoonCake and MoonCake_1 SIP registered"
        moonCake.enableSIP()
        moonCake_1.enableSIP()
        moonCake.registerSip("TLS", true, "", dma.ip, "", sipUri, "")
        moonCake_1.registerSip("TLS", true, "", dma.ip, "", sipUri_1, "")


        then: "MoonCake place call to MoonCake_1 with call rate 4096"
        logger.info("===============Start SIP Call with call rate 4096===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(sipUri_1, CallType.SIP, 4096)
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        then: "Mute audio for MoonCake_1 and last 3 minutes"
        moonCake_1.muteAudio(true)
        pauseTest(200)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        //need api to captureScreen for MoonCake

        then: "Mute audio for MoonCake and last 3 minutes"
        moonCake.muteAudio(true)
        pauseTest(200)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        //need api to captureScreen for MoonCake
        logger.info("===============Successfully start SIP Call with call rate 4096===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(2)
    }
}
