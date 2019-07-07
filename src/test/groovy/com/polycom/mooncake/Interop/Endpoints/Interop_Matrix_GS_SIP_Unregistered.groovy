package com.polycom.mooncake.Interop.Endpoints

import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Dancy Li on 2019-04-26.
 * This case is to verify the matrix testing with GS by SIP while both endpoints are not registered
 * to any call server.
 */

class Interop_Matrix_GS_SIP_Unregistered extends MoonCakeSystemTestSpec {
    @Shared
    GroupSeries groupSeries

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("no")
        groupSeries.enableSIP()

        moonCake.enableSIP()
        moonCake.setEncryption("off")

    }

    def cleanupSpec() {
        groupSeries.api().startReboot() //make sure the GS cannot crash in the later tests
        pauseTest(360)
        testContext.releaseSut(groupSeries)
        moonCake.init()
    }

    @Unroll
    def "Place call from MoonCake to GroupSeries with both SIP unregistered and call rate #CallRate Kbps"(int CallRate,
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

        //when: "Collect MoonCake performance data"
        //need API to collect Mooncake performance data

        then: "MoonCake place SIP call to Group Series"
        logger.info("===============Start SIP Call with call rate " + CallRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(groupSeries.ip, CallType.SIP, CallRate)
        }
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
        logger.info("===============Successfully started SIP Call with call rate " + CallRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        CallRate | expectedProtocol | expectedResolution_PVTX | expectedResolution_CVTX | expectedResolution_PVTX_Sending | expectedResolution_CVRX | expectedResolution_PVTX_Receiving
        256      | "H.264High"      | "640x360"               | "1280x720:128"          | "320x180"                       | "1280x720"              | "640x360"
        512      | "H.264High"      | "1280x720"              | "1280x720:192"          | "640x360"                       | "1280x720"              | "1280x720"
        768      | "H.264High"      | "1280x720"              | "1280x720:256"          | "1280x720"                      | "1280x720"              | "1280x720"
        1024     | "H.264High"      | "1280x720"              | "1920x1080:384"         | "1280x720"                      | "1920x1080"             | "1280x720"
        1536     | "H.264High"      | "1920x1080"             | "1920x1080:512"         | "1280x720"                      | "1920x1080"             | "1280x720"
        2048     | "H.264High"      | "1920x1080"             | "1920x1080:768"         | "1280x720"                      | "1920x1080"             | "1280x720"
        3072     | "H.264High"      | "1920x1080"             | "1920x1080:1472"        | "1920x1080"                     | "1920x1080"             | "1920x1080"
    }

    @Unroll
    def "Place call from Group Series to MoonCake with both SIP unregistered and call rate 128 Kbps"() {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        when: "Set MoonCake call rate"
        moonCake.setCallRate(128)

        then: "Group Series place SIP call to MoonCake"
        logger.info("===============Start SIP Call with call rate 128 " + "===============")
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(moonCake.ip, CallType.SIP, 128)  //audio call only when GS call rate 128
        }

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        logger.info("===============Successfully started SIP Call with call rate 128 " + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)
    }

    @Unroll
    def "Place call from Group Series to MoonCake with both SIP unregistered and call rate #CallRate Kbps"(int CallRate,
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

        //when: "Collect MoonCake performance data"
        //need API to collect Mooncake performance data

        then: "Group Series place SIP call to MoonCake"
        logger.info("===============Start SIP Call with call rate " + CallRate + "===============")
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(moonCake.ip, CallType.SIP, CallRate)
        }
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
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Receiving}:--:--:0:--")
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

        then: "Push content from MoonCake to Group Series"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Sending}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "${expectedProtocol}:${expectedResolution_CVTX}:--:0:--")

        then: "Stop content"
        groupSeries.stopContent()

        then: "Verify the media statistics of MoonCake after stop content"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        logger.info("===============Successfully started SIP Call with call rate " + CallRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        CallRate | expectedProtocol | expectedResolution_PVTX | expectedResolution_CVTX | expectedResolution_PVTX_Sending | expectedResolution_CVRX | expectedResolution_PVTX_Receiving
        384      | "H.264High"      | "640x360"               | "1280x720:128"          | "640x360"                       | "1280x720"              | "640x360"
        1920     | "H.264High"      | "1920x1080"             | "1920x1080:768"         | "1280x720"                      | "1920x1080"             | "1280x720"
        4096     | "H.264High"      | "1920x1080"             | "1920x1080:1984"        | "1920x1080"                     | "1920x1080"             | "1920x1080"
    }
}
