package com.polycom.mooncake.Interop.Endpoints

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Dancy Li on 2019-04-26.
 * This case is to verify the matrix testing with GS by H.323 while both endpoints are registered
 * to DMA.
 */

class Interop_Matrix_GS_H323 extends MoonCakeSystemTestSpec {
    @Shared
    GroupSeries groupSeries

    @Shared
    Dma dma

    @Shared
    String gs_h323name

    @Shared
    String gs_e164

    @Shared
    String mooncake_e164

    @Shared
    String mooncake_h323name

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("no")
        groupSeries.enableH323()
        moonCake.enableH323()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
        def gsDialString = generateDialString(groupSeries)
        def mcDialString = generateDialString(moonCake)
        gs_h323name = gsDialString.h323Name
        gs_e164 = gsDialString.e164Number
        mooncake_e164 = mcDialString.e164Number
        mooncake_h323name = mcDialString.h323Name
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    @Unroll
    def "Place call from MoonCake to GroupSeries with both H323 registered and call rate #CallRate Kbps"(int CallRate,
                                                                                                         String expectedProtocol,
                                                                                                         String expectedResolution_PVTX,
                                                                                                         String expectedResolution_CVTX,
                                                                                                         String expectedResolution_PVTX_Sending,
                                                                                                         String expectedResolution_CVRX,
                                                                                                         String expectedResolution_PVTX_Receiving) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        when: "Set MoonCake call rate"
        moonCake.setCallRate(CallRate)

        then: "Set both endpoints H323 registered"
        moonCake.registerGk(true, false, dma.ip, mooncake_h323name, mooncake_e164, "", "")
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)

        //when: "Collect MoonCake performance data"
        //need API to collect Mooncake performance data

        then: "MoonCake place H323 call to Group Series"
        logger.info("===============Start H323 Call with call rate " + CallRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(gs_e164, CallType.H323, CallRate)
        }

        then: "Verify MoonCake's call rate"
        assert moonCake.getCallSettings().call_rate == CallRate

        then: "Set MoonCake video mute"
        moonCake.muteVideo(true)

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")

        then: "Push content from MoonCake to Group Series"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Sending}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "${expectedProtocol}:${expectedResolution_CVTX}:--:0:--")

        then: "Stop content and unmute video"
        moonCake.stopContent()
        moonCake.muteVideo(false)

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")

        then: "Push content from Group Series to MoonCake"
        groupSeries.playHdmiContent()

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Receiving}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "${expectedProtocol}:${expectedResolution_CVRX}:--:--:0:--")

        then: "Stop content"
        groupSeries.stopContent()

        then: "Verify the media statistics of MoonCake after stop content"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        logger.info("===============Successfully started H323 Call with call rate " + CallRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        CallRate | expectedProtocol | expectedResolution_PVTX | expectedResolution_CVTX | expectedResolution_PVTX_Sending | expectedResolution_CVRX | expectedResolution_PVTX_Receiving
        256      | "H.264High"      | "640x360"               | "1280x720:128"          | "320x180"                       | "1280x720"              | "640x360"
        384      | "H.264High"      | "640x360"               | "1280x720:128"          | "640x360"                       | "1280x720"              | "640x360"
        512      | "H.264High"      | "1280x720"              | "1280x720:192"          | "640x360"                       | "1280x720"              | "1280x720"
        1536     | "H.264High"      | "1920x1080"             | "1920x1080:512"         | "1280x720"                      | "1920x1080"             | "1280x720"
    }

    @Unroll
    def "Place call from Group Series to MoonCake with both H323 registered and call rate 128 Kbps"() {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        when: "Set MoonCake call rate"
        moonCake.setCallRate(128)

        then: "Set both endpoints H323 registered"
        moonCake.registerGk(true, false, dma.ip, mooncake_h323name, mooncake_e164, "", "")
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)

        then: "Group Series place H323 call to MoonCake"
        logger.info("===============Start H323 Call with call rate 128 " + "===============")
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(mooncake_e164, CallType.H323, 128)  //audio call only when GS call rate 128
        }

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        logger.info("===============Successfully started H323 Call with call rate 128 " + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)
    }

    @Unroll
    def "Place call from MoonCake to GroupSeries with both H323 registered and call rate 64 Kbps"() {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        when: "Set MoonCake call rate"
        moonCake.setCallRate(64)

        then: "Set both endpoints H323 registered"
        moonCake.registerGk(true, false, dma.ip, mooncake_h323name, mooncake_e164, "", "")
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)

        then: "Group Series place H323 call to MoonCake"
        logger.info("===============Start H323 Call with call rate 64 " + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(gs_e164, CallType.H323, 64)  //audio call only when call rate 64
        }

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        logger.info("===============Successfully started H323 Call with call rate 64 " + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)
    }

    @Unroll
    def "Place call from Group Series to MoonCake with both H323 registered and call rate #CallRate Kbps"(int CallRate,
                                                                                                          String expectedProtocol,
                                                                                                          String expectedResolution_PVTX,
                                                                                                          String expectedResolution_CVTX,
                                                                                                          String expectedResolution_PVTX_Sending,
                                                                                                          String expectedResolution_CVRX,
                                                                                                          String expectedResolution_PVTX_Receiving) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        when: " set MoonCake call rate"
        moonCake.setCallRate(CallRate)

        then: "Set both MoonCake and Group Series to H323 registered"
        moonCake.registerGk(true, false, dma.ip, mooncake_h323name, mooncake_e164, "", "")
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)

        //when: "Collect MoonCake performance data"
        //need API to collect Mooncake performance data

        then: "MoonCake place H323 call to Group Series"
        logger.info("===============Start H323 Call with call rate " + CallRate + "===============")
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(mooncake_e164, CallType.H323, CallRate)
        }

        then:
        "verify MoonCake's call rate same as " + CallRate
        assert moonCake.getCallSettings().call_rate == CallRate

        then: "Set MoonCake Video Mute"
        moonCake.muteVideo(true)

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")

        then: "Push content from Group Series to MoonCake"
        groupSeries.playHdmiContent()

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Sending}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "${expectedProtocol}:${expectedResolution_CVRX}:--:--:0:--")

        then: "Stop content and unmute video"
        groupSeries.stopContent()
        moonCake.muteVideo(false)

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")

        then: "Push content from MoonCake to GroupSeries"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedResolution_PVTX_Sending}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "${expectedProtocol}:${expectedResolution_CVTX}:--:0:--")
        logger.info("===============Successfully started H323 Call with call rate " + CallRate + "===============")

        then: "Stop content"
        moonCake.stopContent()

        then: "Verify the media statistics of MoonCake after stop content"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        logger.info("===============Successfully started H323 Call with call rate " + CallRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        CallRate | expectedProtocol | expectedResolution_PVTX | expectedResolution_CVTX | expectedResolution_PVTX_Sending | expectedResolution_CVRX | expectedResolution_PVTX_Receiving
        768      | "H.264High"      | "1280x720"              | "1280x720:256"          | "1280x720"                      | "1280x720"              | "1280x720"
        1024     | "H.264High"      | "1280x720"              | "1920x1080:384"         | "1280x720"                      | "1920x1080"             | "1280x720"
        1920     | "H.264High"      | "1920x1080"             | "1920x1080:768"         | "1280x720"                      | "1920x1080"             | "1280x720"
        2048     | "H.264High"      | "1920x1080"             | "1920x1080:768"         | "1280x720"                      | "1920x1080"             | "1280x720"
        3072     | "H.264High"      | "1920x1080"             | "1920x1080:1472"        | "1920x1080"                     | "1920x1080"             | "1920x1080"
        4096     | "H.264High"      | "1920x1080"             | "1920x1080:1984"        | "1920x1080"                     | "1920x1080"             | "1920x1080"
    }
}
