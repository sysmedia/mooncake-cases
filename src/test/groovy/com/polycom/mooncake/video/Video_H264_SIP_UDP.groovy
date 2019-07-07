package com.polycom.mooncake.video

/**
 * Created by nshi on 4/29/2019.
 */

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared

/**
 * Created by Nancy Shi on 2019-04-22.
 */

//Need manually change SAT as specific Mooncake by "Nancy" keyword in MoonCakeSystemTestSpec.groovy

class Video_H264_SIP_UDP extends MoonCakeSystemTestSpec{
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
        moonCake.registerSip("UDP", false, "", "", "", "", "")
    }

    def "Verify Video Protocol In SIP Call With Specified protocol H264 BaseLine"(String dialString,
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
        [dialString, sipTransProtocol, videoProtocol, expectedProtocol, callRate] << getTestData()
    }

    def getTestData() {
        def rtn = []
        callRateList.each {
            rtn << [hdxSipURI,"UDP","h264", "H.264", it]
        }
        return rtn
    }
}
