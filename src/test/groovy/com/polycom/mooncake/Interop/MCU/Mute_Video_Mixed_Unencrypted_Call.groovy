package com.polycom.mooncake.Interop.MCU

import com.polycom.auto.resources.enums.SipTransportProtocol
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared

/**
 * Created by Gary Wang on 2019-06-12.
 *
 #This case is to verify the scenario while MoonCake turn off its video in mixed unencrypted call
 #Far end can see the muted video picture after SUT turn off its video
 #The layout is correct while SUT receives content after it turns off the local video
 #The local video status is correct when SUT turns on/off the video
 */
class Mute_Video_Mixed_Unencrypted_Call extends MoonCakeSystemTestSpec{
    @Shared
    Dma dma

    @Shared
    String vmr = "204863"

    @Shared
    String h323Name1 = "automooncake2048"

    @Shared
    String e164Num1 = "2198108"

    @Shared
    String moonsipUri = "mooncake"

    @Shared
    def gs_h323name, ep_h323name

    @Shared
    def gs_e164, ep_e164

    @Shared
    String gs_sip_username, ep_sip_username

    @Shared
    MoonCake moonCake111

    @Shared
    GroupSeries groupSeries

    @Shared
    int callRate = 2048


    def setupSpec() {
        moonCake = testContext.bookSut(MoonCake.class, keyword)
        moonCake.updateCallSettings(callRate, "off", true, false, true);
        moonCake111 = testContext.bookSut(MoonCake.class, "backup")
        moonCake111.updateCallSettings(callRate, "off", true, false, true);
        dma = testContext.bookSut(Dma.class, keyword)
        groupSeries = testContext.bookSut(GroupSeries.class, "GS700")
        groupSeries.init()
        groupSeries.setEncryption("no")

        def dialString = generateDialString(moonCake)
        moonsipUri = dialString.sipUri
        h323Name1 = dialString.h323Name
        e164Num1 = dialString.e164Number

        def dialString2 = generateDialString(groupSeries)
        gs_h323name = dialString2.h323Name
        gs_e164 = dialString2.e164Number
        gs_sip_username = dialString2.sipUri

        def dialString3 = generateDialString(moonCake111)
        ep_h323name = dialString3.h323Name
        ep_e164 = dialString3.e164Number
        ep_sip_username = dialString3.sipUri

        groupSeries.enableH323()
        groupSeries.enableSIP()
        groupSeries.registerGk(gs_h323name,gs_e164,dma.ip)
        groupSeries.registerSip(gs_sip_username, centralDomain, "",dma.ip,SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TCP)


    }

    def cleanupSpec() {
        testContext.releaseSut(dma)
        testContext.releaseSut(moonCake111)
        testContext.releaseSut(groupSeries)
    }
    def "Verify MoonCake joins unencrypted VMR by H323"() {
        setup:
        moonCake.hangUp()
        moonCake111.hangUp()
        groupSeries.hangUp()

        moonCake.enableH323()
        moonCake111.enableH323()
        pauseTest(5)
        moonCake.registerGk(true, false, dma.ip, h323Name1, e164Num1, "", "")
        moonCake111.registerGk(true,false, dma.ip, ep_h323name, ep_e164,"","")
       // moonCake111.registerGk(true, false, dma.ip, ep_h323name, ep_e164, "", "")
        //moonCake111.registerSip("TCP", true, "", dma.ip, ep_sip_username, ep_sip_username, "")
        pauseTest(10)
        when: "Place call on the mooncake with call rate 2048Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(vmr,CallType.SIP,callRate)
            moonCake.placeCall(vmr,CallType.H323,callRate)
            moonCake111.placeCall(vmr,CallType.H323,callRate)
            pauseTest(3)
        }
        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        then: "moon cake mute video"
        moonCake.muteVideo(true)

        then: "Push content on the Group"
        groupSeries.playHdmiContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--")

        then: "Push content on the mooncake"
        moonCake.pushContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")


        then: "mooncake unmute video"
        moonCake.muteVideo(false)

        then: "mooncake stop content"
        moonCake.stopContent()

        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        moonCake111.hangUp()
        groupSeries.hangUp()
        pauseTest(10)

    }

    def "Verify MoonCake joins unencrypted VMR by SIP"() {
        setup:
        moonCake.hangUp()
        moonCake111.hangUp()
        groupSeries.hangUp()

        moonCake.enableSIP()
        moonCake111.enableSIP()
        pauseTest(5)
        moonCake.registerSip("TCP", true, "", dma.ip, moonsipUri, moonsipUri, "")
        moonCake111.registerSip("TCP", true, "", dma.ip, ep_sip_username, ep_sip_username, "")
        pauseTest(10)
        when: "Place call on the mooncake with call rate 2048Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(vmr,CallType.H323,callRate)
            moonCake.placeCall(vmr,CallType.SIP,callRate)
            moonCake111.placeCall(vmr,CallType.SIP,callRate)
            pauseTest(3)
        }
        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        then: "moon cake mute video"
        moonCake.muteVideo(true)

        then: "Push content on the Group"
        groupSeries.playHdmiContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--")

        then: "Push content on the mooncake"
        moonCake.pushContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")


        then: "mooncake unmute video"
        moonCake.muteVideo(false)

        then: "mooncake stop content"
        moonCake.stopContent()

        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        moonCake111.hangUp()
        groupSeries.hangUp()
        pauseTest(10)

    }


}
