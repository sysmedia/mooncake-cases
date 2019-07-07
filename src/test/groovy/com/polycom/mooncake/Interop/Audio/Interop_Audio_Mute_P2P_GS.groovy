package com.polycom.mooncake.Interop.Audio

import com.polycom.auto.resources.enums.SipTransportProtocol
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Dancy Li on 5/29/2019
 * This case is to verify the call between MoonCake and GS should work well when
 * MoonCake mutes audio for more than 3 minutes;
 * Far end mutes audio for more than 3 minutes
 * Different call type: H323, SIP
 */

class Interop_Audio_Mute_P2P_GS extends MoonCakeSystemTestSpec {
    @Shared
    GroupSeries groupSeries

    @Shared
    Dma dma

    @Shared
    String gs_h323name

    @Shared
    String gs_e164

    @Shared
    String mc_h323Name

    @Shared
    String mc_e164Num

    @Shared
    String sipUri

    @Shared
    String gs_sip_username

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("no")
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
        def gsDialString = generateDialString(groupSeries)
        def mcDialString = generateDialString(moonCake)
        gs_h323name = gsDialString.h323Name
        gs_e164 = gsDialString.e164Number
        mc_h323Name = mcDialString.h323Name
        mc_e164Num = mcDialString.e164Number
        sipUri = generateDialString(moonCake).sipUri
        gs_sip_username = generateDialString(groupSeries).sipUri
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    @Unroll
    def "Verify H323 P2P call when MoonCake call Group Series and mute audio with call rate 2048 Kbps"() {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        when: "Set both MoonCake and Group Series H323 registered"
        moonCake.enableH323()
        groupSeries.enableH323()
        moonCake.registerGk(true, false, dma.ip, mc_h323Name, mc_e164Num, "", "")
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)

        then: "MoonCake place call to GroupSeries with call rate 2048"
        logger.info("===============Start H323 Call with call rate 2048===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(gs_e164, CallType.H323, 2048)
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

        //need api to captureScreen for MoonCake

        then: "Mute audio for Group Series and last 3 minutes"
        groupSeries.setAudioMuted(true)
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
    def "Verify SIP P2P call when MoonCake call Group Series and mute audio with call rate 4096 Kbps"() {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        when: "Set both MoonCake Group Series SIP registered"
        moonCake.enableSIP()
        groupSeries.enableSIP()
        moonCake.registerSip("TLS", true, "", dma.ip, "", sipUri, "")
        groupSeries.registerSip(gs_sip_username, centralDomain, "", dma.ip, SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TLS)


        then: "MoonCake place call to GroupSeries with call rate 4096"
        logger.info("===============Start H323 Call with call rate 4096===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(gs_sip_username, CallType.SIP, 4096)
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        then: "Mute audio for Group Series and last 3 minutes"
        groupSeries.setAudioMuted(true)
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
        logger.info("===============Successfully start SIP Call with call rate 4096===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(2)
    }


}
