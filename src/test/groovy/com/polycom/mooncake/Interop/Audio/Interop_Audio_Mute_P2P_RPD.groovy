package com.polycom.mooncake.Interop.Audio

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.RpdWin
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Dancy Li on 5/29/2019
 * This case is to verify the call between MoonCake and RPD should work well when
 * MoonCake mutes audio for more than 3 minutes;
 * Far end mutes audio for more than 3 minutes
 * Different call type: H323, SIP
 */

class Interop_Audio_Mute_P2P_RPD extends MoonCakeSystemTestSpec{
    @Shared
    RpdWin rpdWin

    @Shared
    Dma dma

    @Shared
    String sipUri

    @Shared
    String rpd_sip_username

    @Shared
    String mc_h323Name

    @Shared
    String mc_e164Num

    @Shared
    String rpd_e164

    @Shared
    String rpd_h323Name

    def setupSpec() {
        rpdWin = testContext.bookSut(RpdWin.class, keyword)
        rpdWin.init()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
        sipUri = generateDialString(moonCake)
        rpd_sip_username = generateDialString(rpdWin)
        def mcDialString = generateDialString(moonCake)
        def rpdDialString = generateDialString(rpdWin)
        mc_h323Name = mcDialString.h323Name
        mc_e164Num = mcDialString.e164Number
        rpd_h323Name = rpdDialString.h323Name
        rpd_e164 = rpdDialString.e164Number
    }

    def cleanupSpec() {
        testContext.releaseSut(rpdWin)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    @Unroll
    def "Verify H323 P2P call when MoonCake call RPD and mute audio with call rate 2048 Kbps"() {
        setup:
        moonCake.hangUp()
        rpdWin.hangUp()

        when: "Set both MoonCake and RPD H323 registered"
        moonCake.enableH323()
        rpdWin.enableH323()
        moonCake.registerGk(true, false, dma.ip, mc_h323Name, mc_e164Num, "", "")
        rpdWin.registerH323(dma.ip, rpd_h323Name, rpd_e164)
        pauseTest(3)

        then: "MoonCake place call to RPD with call rate 2048"
        logger.info("===============Start H323 Call with call rate 2048===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(rpd_e164, CallType.H323, 2048)
        }

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

        then: "Mute audio for RPD and last 3 minutes"
        rpdWin.muteAudio()
        pauseTest(200)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        //need api to captureScreen for MoonCake
        logger.info("===============Successfully start H.323 Call with call rate 2048===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(2)
    }

    @Unroll
    def "Verify SIP P2P call when MoonCake call RPD and mute audio with call rate 1920 Kbps"() {
        setup:
        moonCake.hangUp()
        rpdWin.hangUp()

        when: "Set both MoonCake and RPD SIP registered"
        moonCake.enableSIP()
        rpdWin.enableSip()
        moonCake.registerSip("TLS", true, "", dma.ip, "", sipUri, "")
        rpdWin.registersip(dma.ip, rpd_sip_username, "", "", "", "TLS")


        then: "MoonCake place call to RPD with call rate 1920"
        logger.info("===============Start H323 Call with call rate 1920===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(rpd_sip_username, CallType.SIP, 1920)
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        then: "Mute audio for RPD and last 3 minutes"
        rpdWin.muteAudio()
        pauseTest(200)

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
        logger.info("===============Successfully start SIP Call with call rate 1920===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(2)
    }
}
