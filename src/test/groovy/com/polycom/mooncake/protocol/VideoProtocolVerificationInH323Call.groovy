package com.polycom.mooncake.protocol

import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by taochen on 2019-04-04.
 */
class VideoProtocolVerificationInH323Call extends MoonCakeSystemTestSpec {
    @Shared
    GroupSeries groupSeries

    @Shared
    String hdxIp = "172.21.118.151"

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("no")
        moonCake.enableH323()
        moonCake.setEncryption("off")
    }

    def cleanupSpec() {
        groupSeries.api().setStereoEnabled(true)
        testContext.releaseSut(groupSeries)
    }

    def getTestData() {
        def rtn = []
        callRateList.each {
            rtn << ["h264", "H.264High", it]
        }
        return rtn
    }

    @Unroll
    def "Verify Video Protocol In H323 Call With protocol #expectedProtocol And In Call Rate #callRate Kbps"(String videoProtocol,
                                                                                                             String expectedProtocol,
                                                                                                             int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()
        groupSeries.setVideoProtocol("reset")
        groupSeries.api().setStereoEnabled(false)

        when: "Set the GroupSeries video protocol"
        if (videoProtocol != null) {
            groupSeries.setVideoProtocol(videoProtocol)
        } else {
            groupSeries.setVideoProtocol("reset")
        }

        then: "MoonCake place call to the GroupSeries with various call rate and then verify the media statistics"

        logger.info("===============Start H323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(groupSeries, CallType.H323, callRate)
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:--:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")


        cleanup:
        moonCake.hangUp()
        pauseTest(2)

        where:
        [videoProtocol, expectedProtocol, callRate] << getTestData()
    }

    //Since GS cannot force set the video protocol as H264 (base profile), we will have to switch the tests on a HDX
    @Ignore
    def "Verify Video Protocol In H323 Call With Specified protocol H264"(String videoProtocol,
                                                                          String expectedProtocol,
                                                                          int[] callRates) {
        setup:
        moonCake.hangUp()

        when: "Set the HDX video protocol"
        //it will be made in the future

        then: "MoonCake place call to the GroupSeries with various call rate and then verify the media statistics"
        callRates.each {
            logger.info("===============Start H323 Call with call rate " + it + "===============")
            moonCake.placeCall(hdxIp, CallType.H323, it)

            then: "Verify the media statistics during the call"
            verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:--:--:--:--:--")
            logger.info("===============Successfully start H323 Call with call rate " + it + "===============")
            moonCake.hangUp()
            pauseTest(5)
        }

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
        pauseTest(5)

        where:
        videoProtocol | expectedProtocol | callRates
        "h264"        | "H.264"          | callRateList
    }

}
