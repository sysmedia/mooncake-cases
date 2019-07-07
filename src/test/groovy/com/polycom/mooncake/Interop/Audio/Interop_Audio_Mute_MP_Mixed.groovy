package com.polycom.mooncake.Interop.Audio

import com.polycom.auto.resources.enums.SipTransportProtocol
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.RpdWin
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared

/**
 * Created by Dancy Li on 5/30/2019
 * This case is to verify MoonCake should work well in AVC and SVC mixed conference when
 * MoonCake mutes audio for more than 3 minutes
 * Far end mutes audio for more than 3 minutes
 *
 *  Environment: MCU register H.323 and SIP to DMA, DMA integrated with MCU for both H.323 and SIP
 *  Conference profile: AVC and SVC mixed, Line rate 4096k + Encryption auto
 */

class Interop_Audio_Mute_MP_Mixed extends MoonCakeSystemTestSpec {
    @Shared
            dma

    @Shared
    RpdWin rpdWin

    @Shared
    GroupSeries groupSeries

    @Shared
    MoonCake moonCake_1

    @Shared
    String rpd_sip_username

    @Shared
    String rpd_e164

    @Shared
    String rpd_h323Name

    @Shared
    String gs_h323name

    @Shared
    String gs_e164

    @Shared
    String gs_sip_username

    @Shared
    String mc_e164

    @Shared
    String mc_h323Name

    @Shared
    String mc_1_e164

    @Shared
    String mc_1_h323Name

    @Shared
    String vmr = "409699"   //the vmr number is pre-created on DMA as AVC and SVC mixed conference

    @Shared
    String sipUri

    @Shared
    String sipUri_1

    def setupSpec() {
        dma = testContext.bookSut(Dma.class, keyword)

        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()

        rpdWin = testContext.bookSut(RpdWin.class, keyword)
        rpdWin.init()

        moonCake_1 = testContext.bookSut(MoonCake.class, keyword, "backup")
        moonCake_1.init()

        groupSeries.setEncryption("no")
        moonCake.setEncryption("off")
        moonCake_1.setEncryption("no")

        def mcDialString = generateDialString(moonCake)
        def mc_1_DialString = generateDialString(moonCake_1)
        mc_e164 = mcDialString.e164Number
        mc_h323Name = mcDialString.h323Name
        mc_1_e164 = mc_1_DialString.e164Number
        mc_1_h323Name = mc_1_DialString.h323Name
        sipUri = mcDialString.sipUri
        sipUri_1 = mc_1_DialString.sipUri

        def gsDialString = generateDialString(groupSeries)
        gs_h323name = gsDialString.h323Name
        gs_e164 = gsDialString.e164Number
        gs_sip_username = generateDialString(groupSeries).sipUri

        def rpdDialString = generateDialString(rpdWin)
        rpd_h323Name = rpdDialString.h323Name
        rpd_e164 = rpdDialString.e164Number
        rpd_sip_username = generateDialString(rpdWin)
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(rpdWin)
        moonCake_1.init()
        testContext.releaseSut(moonCake_1)
        moonCake.init()
    }

    def "Verify MoonCake call into conference by H323 and mute audio with call rate 2048 or 1024Kbps"() {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()
        moonCake_1.hangUp()
        rpdWin.hangUp()

        when: "Set both RPD and Group Series SIP registered"
        groupSeries.enableSIP()
        rpdWin.enableSip()
        groupSeries.registerSip(gs_sip_username, centralDomain, "", dma.ip, SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TLS)
        rpdWin.registersip(dma.ip, rpd_sip_username, "", "", "", "TLS")
        pauseTest(3)

        then: "Set MoonCake_1 SIP registered"
        moonCake_1.enableSIP()
        moonCake_1.registerSip("TLS", true, "", dma.ip, "", sipUri_1, "")


        then: "Set MoonCake H323 registered"
        moonCake.enableH323()
        moonCake.registerGk(true, false, dma.ip, mc_h323Name, mc_e164, "", "")
        pauseTest(3)

        then: "MoonCake place call to vmr conference "
        logger.info("===============Start H.323 Call with call rate 2048 or 1024===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(vmr, CallType.H323, 2048)
            moonCake_1.placeCall(vmr, CallType.SIP, 1024)
            groupSeries.placeCall(vmr, CallType.SIP, 1024)
            rpdWin.placeCall(vmr, CallType.SIP, 1024)
            pauseTest(3)
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

        then: "Mute audio for MoonCake_1, Group Series, and RPD, and last 3 minutes"
        groupSeries.setAudioMuted(true)
        moonCake_1.muteAudio(true)
        rpdWin.muteAudio()
        pauseTest(200)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        //need api to captureScreen for MoonCake
        logger.info("===============Successfully start H323 Call with call rate 2048 or 1024===============")

        cleanup:
        moonCake_1.hangUp()
        groupSeries.hangUp()
        rpdWin.hangUp()
        moonCake.hangUp()
        pauseTest(2)
    }

    def "Verify MoonCake call into conference by SIP and mute audio with call rate 2048 or 1024Kbps"() {

        setup:
        moonCake.hangUp()
        groupSeries.hangUp()
        moonCake_1.hangUp()
        rpdWin.hangUp()

        when: "Set both RPD and Group Series H323 registered"
        groupSeries.enableH323()
        rpdWin.enableH323()
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)
        rpdWin.registerH323(dma.ip, rpd_h323Name, rpd_e164)
        pauseTest(3)

        then: "Set MoonCake SIP registered"
        moonCake.enableSIP()
        moonCake.registerSip("TLS", true, "", dma.ip, "", sipUri, "")

        then: "Set MoonCake_1 H323 registered"
        moonCake_1.enableH323()
        moonCake_1.registerGk(true, false, dma.ip, mc_1_h323Name, mc_1_e164, "", "")


        then: "MoonCake place call to vmr conference "
        logger.info("===============Start SIP Call with call rate 2048 or 1024===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(vmr, CallType.SIP, 2048)
            moonCake_1.placeCall(vmr, CallType.H323, 1024)
            groupSeries.placeCall(vmr, CallType.H323, 1024)
            rpdWin.placeCall(vmr, CallType.H323, 1024)
            pauseTest(3)
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

        then: "Mute audio for MoonCake_1, Group Series, and RPD, and last 3 minutes"
        groupSeries.setAudioMuted(true)
        moonCake_1.muteAudio(true)
        rpdWin.muteAudio()
        pauseTest(200)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        //need api to captureScreen for MoonCake
        logger.info("===============Successfully start SIP Call with call rate 2048 or 1024===============")

        cleanup:
        moonCake_1.hangUp()
        groupSeries.hangUp()
        rpdWin.hangUp()
        moonCake.hangUp()
        pauseTest(2)
    }


}
