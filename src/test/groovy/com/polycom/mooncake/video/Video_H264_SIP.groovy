package com.polycom.mooncake.video

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Nancy Shi on 2019-04-22.
 */

//Need manually change SAT as specific Mooncake by "Nancy" keyword in MoonCakeSystemTestSpec.groovy

class Video_H264_SIP extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    String sipUri = "mooncake"

    @Shared
    String hdxSipURI = "99250"

    def setupSpec() {
        moonCake.enableSIP()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
    }

    def cleanupSpec() {
        testContext.releaseSut(dma)
        moonCake.registerSip("TCP", false, "", "", "", "", "")
    }
    @Unroll
    def "SIP Call in TCP transport protocol with dial string #dialString and call rate #callRate"(String dialString,
                                                                                                  String sipTransProtocol,
                                                                                                  String videoProtocol,
                                                                                                  String expectedProtocol,
                                                                                                  int callRate) {
        setup:
        moonCake.hangUp()

        when: "Set the mooncake SIP transport protocol with registering the SIP server"
        //it will be made in the future.Now it is set by manually as H264 Baseprofile: pbox config H264ProfileName 0 BaseLine
        moonCake.registerSip(sipTransProtocol, true, "", dma.ip, "", sipUri, "")

        then: "MoonCake place call to the HDX with various call rate and then verify the media statistics"

        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP, callRate)
            pauseTest(15)
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "G.722.1C:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:--:--:--:--:--")
        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(15)

        where:
        [dialString, sipTransProtocol, videoProtocol, expectedProtocol, callRate] << getTestData_1()
    }

    def getTestData_1() {
        def rtn = []
        callRateList.each {
            rtn << [hdxSipURI, "TCP", "h264", "H.264", it]
        }
        return rtn
    }


    def "SIP Call in UDP transport protocol with dial String #dialString and call rate #callRate"(String dialString,
                                                                                                  String sipTransProtocol,
                                                                                                  String videoProtocol,
                                                                                                  String expectedProtocol,
                                                                                                  int callRate) {
        setup:
        moonCake.hangUp()

        when: "Set the mooncake SIP transport protocol with registering the SIP server"
        //it will be made in the future.Now it is set by manually as H264 Baseprofile: pbox config H264ProfileName 0 BaseLine
        moonCake.registerSip(sipTransProtocol, true, "", dma.ip, "", sipUri, "")

        then: "MoonCake place call to the HDX with various call rate and then verify the media statistics"

        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP, callRate)
            pauseTest(15)
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "G.722.1C:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:--:--:--:--:--")
        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(15)

        where:
        [dialString, sipTransProtocol, videoProtocol, expectedProtocol, callRate] << getTestData_2()
    }

    def getTestData_2() {
        def rtn = []
        callRateList.each {
            rtn << [hdxSipURI, "UDP","h264", "H.264", it]
        }
        return rtn
    }
}
