package com.polycom.mooncake.protocol

import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Nancy Shi on 2019-04-22.
 */

//Need manually change SAT as specific Mooncake by "Nancy" keyword in MoonCakeSystemTestSpec.groovy

class Video_H264_H323 extends MoonCakeSystemTestSpec {
    String hdxIp = "172.21.126.250"

    def setupSpec() {
        moonCake.enableH323()
        moonCake.setEncryption("off")
    }

    def "Verify Video Protocol In H323 Call With Specified protocol H264 BaseLine"(String videoProtocol,
                                                                                   String expectedProtocol,
                                                                                   int callRate) {
        setup:
        moonCake.hangUp()

        when: "Set the HDX video protocol"
        //it will be made in the future.Now it is set by manually as H264 Baseprofile: pbox config H264ProfileName 0 BaseLine

        then: "MoonCake place call to the HDX with various call rate and then verify the media statistics"

        logger.info("===============Start H323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(hdxIp, CallType.H323, callRate)
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:--:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        [videoProtocol, expectedProtocol, callRate] << getTestData()
    }

    def getTestData() {
        def rtn = []
        callRateList.each {
            rtn << ["h264", "H.264", it]
        }
        return rtn
    }
}
