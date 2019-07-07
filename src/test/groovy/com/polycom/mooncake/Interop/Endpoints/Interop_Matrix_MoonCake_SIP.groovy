package com.polycom.mooncake.Interop.Endpoints

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Dancy Li on 2019-04-30.
 * This case is to verify the matrix testing by SIP while both MoonCake are registered
 * to DMA server
 */

class Interop_Matrix_MoonCake_SIP extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    MoonCake moonCake_1

    @Shared
    String sipUri

    @Shared
    String sipUri_1

    def setupSpec() {
        moonCake.enableSIP()
        moonCake.setEncryption("off")
        moonCake_1 = testContext.bookSut(MoonCake.class, keyword, "backup")
        moonCake_1.init()
        moonCake_1.enableSIP()
        moonCake_1.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
        sipUri = generateDialString(moonCake).sipUri
        sipUri_1 = generateDialString(moonCake_1).sipUri
    }

    def cleanupSpec() {
        moonCake.init()
        moonCake_1.init()
        testContext.releaseSut(moonCake_1)
        testContext.releaseSut(dma)
    }

    @Unroll
    def "Place call from MoonCake to MoonCake_1 with both SIP registered and call rate #CallRate Kbps"(int CallRate,
                                                                                                       String expectedProtocol,
                                                                                                       String expectedResolution_PVTX,
                                                                                                       String expectedResolution_CVTX,
                                                                                                       String expectedResolution_PVTX_Sending,
                                                                                                       String expectedResolution_CVRX,
                                                                                                       String expectedResolution_PVTX_Receiving) {
        setup:
        moonCake.hangUp()
        moonCake_1.hangUp()
        moonCake.setCallRate(CallRate)
        moonCake_1.setCallRate(CallRate)

        //when: "Collect MoonCake performance data"
        //need API to collect Mooncake performance data

        when: "Set both MoonCake and MoonCake_1 to SIP registered"
        moonCake.registerSip("TLS", true, "", dma.ip, "", sipUri, "")
        moonCake_1.registerSip("TLS", true, "", dma.ip, "", sipUri_1, "")

        then: "MoonCake place SIP call to MoonCake_1 and Mute MoonCake video"
        logger.info("===============Start SIP Call with call rate " + CallRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(sipUri_1, CallType.SIP, CallRate)
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

        then: "Verify the media statistics of MoonCake_1 during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--", moonCake_1)

        then: "Push content from MoonCake to MoonCake_1"
        moonCake.pushContent()
        pauseTest(5)

        //then: "Adjust MoonCake's camera while sending content"
        //need a API

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Sending}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "${expectedProtocol}:${expectedResolution_CVTX}:--:0:--")

        then: "Verify the media statistics of MoonCake_1 during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Receiving}:--:--:0:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.CVRX, "${expectedProtocol}:${expectedResolution_CVRX}:--:--:0:--", moonCake_1)

        then: "Push content from MoonCake_1 to MoonCake"
        moonCake_1.pushContent()

        //then: "Adjust MoonCake's camera while sending content"
        //need a API

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Receiving}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "${expectedProtocol}:${expectedResolution_CVRX}:--:--:0:--")

        then: "Verify the media statistics of MoonCake_1 during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Sending}:--:--:0:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.CVTX, "${expectedProtocol}:${expectedResolution_CVTX}:--:0:--", moonCake_1)

        then: "Stop content and unmute MoonCake video"
        moonCake_1.stopContent()
        moonCake.muteVideo(false)

        then: "Verify the media statistics of MoonCake after stop content"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")

        then: "Verify the media statistics of MoonCake_1 after stop content"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--", moonCake_1)
        logger.info("===============Successfully started SIP Call with call rate " + CallRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        CallRate | expectedProtocol | expectedResolution_PVTX | expectedResolution_CVTX | expectedResolution_PVTX_Sending | expectedResolution_CVRX | expectedResolution_PVTX_Receiving
        256      | "H.264High"      | "640x360"               | "1280x720:128"          | "320x180"                       | "1280x720"              | "640x360"
        384      | "H.264High"      | "640x360"               | "1280x720:128"          | "640x360"                       | "1280x720"              | "640x360"
        512      | "H.264High"      | "1280x720"              | "1280x720:192"          | "640x360"                       | "1280x720"              | "1280x720"
        768      | "H.264High"      | "1280x720"              | "1280x720:256"          | "1280x720"                      | "1280x720"              | "1280x720"
        1536     | "H.264High"      | "1920x1080"             | "1920x1080:512"         | "1280x720"                      | "1920x1080"             | "1280x720"
        2048     | "H.264High"      | "1920x1080"             | "1920x1080:768"         | "1280x720"                      | "1920x1080"             | "1280x720"
        3072     | "H.264High"      | "1920x1080"             | "1920x1080:1472"        | "1920x1080"                     | "1920x1080"             | "1920x1080"
        4096     | "H.264High"      | "1920x1080"             | "1920x1080:1984"        | "1920x1080"                     | "1920x1080"             | "1920x1080"
    }

    @Unroll
    def "Place call from MoonCake_1 to MoonCake with both SIP registered and call rate 64 Kbps"() {
        setup:
        moonCake.hangUp()
        moonCake_1.hangUp()
        moonCake.setCallRate(64)
        moonCake_1.setCallRate(64)

        when: "Set both MoonCake and MoonCake_1 to SIP registered"
        moonCake.registerSip("TLS", true, "", dma.ip, "", sipUri, "")
        moonCake_1.registerSip("TLS", true, "", dma.ip, "", sipUri_1, "")

        then: "MoonCake place SIP call to MoonCake_1 and Mute MoonCake video"
        logger.info("===============Start SIP Call with call rate 64 " + "===============")
        retry(times: 3, delay: 5) {
            moonCake_1.placeCall(sipUri, CallType.SIP, 64)
        }

        then: "Verify MoonCake's call rate"
        assert moonCake.getCallSettings().call_rate == 64

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        logger.info("===============Successfully started SIP Call with call rate 64 " + "===============")

        cleanup:
        moonCake_1.hangUp()
        pauseTest(5)
    }

    @Unroll
    def "Place call from MoonCake_1 to MoonCake with both SIP registered and call rate #CallRate Kbps"(int CallRate,
                                                                                                       String expectedProtocol,
                                                                                                       String expectedResolution_PVTX,
                                                                                                       String expectedResolution_CVTX,
                                                                                                       String expectedResolution_PVTX_Sending,
                                                                                                       String expectedResolution_CVRX,
                                                                                                       String expectedResolution_PVTX_Receiving) {
        setup:
        moonCake.hangUp()
        moonCake_1.hangUp()
        moonCake.setCallRate(CallRate)
        moonCake_1.setCallRate(CallRate)

        //when: "Collect MoonCake performance data"
        //need API to collect Mooncake performance data

        when: "Set both MoonCake and MoonCake_1 to SIP registered"
        moonCake.registerSip("TLS", true, "", dma.ip, "", sipUri, "")
        moonCake_1.registerSip("TLS", true, "", dma.ip, "", sipUri_1, "")

        then: "MoonCake place SIP call to MoonCake_1 and Mute MoonCake video"
        logger.info("===============Start SIP Call with call rate " + CallRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake_1.placeCall(sipUri, CallType.SIP, CallRate)
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

        then: "Verify the media statistics of MoonCake_1 during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--", moonCake_1)

        then: "Push content from MoonCake_1 to MoonCake"
        moonCake_1.pushContent()

        //then: "Adjust MoonCake's camera while sending content"
        //need a API

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Sending}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "${expectedProtocol}:${expectedResolution_CVTX}:--:0:--")

        then: "Verify the media statistics of MoonCake_1 during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Receiving}:--:--:0:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.CVTX, "${expectedProtocol}:${expectedResolution_CVRX}:--:--:0:--", moonCake_1)

        then: "Push content from MoonCake to MoonCake_1"
        moonCake.pushContent()
        pauseTest(5)

        //then: "Adjust MoonCake's camera while sending content"
        //need a API

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Receiving}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "${expectedProtocol}:${expectedResolution_CVRX}:--:--:0:--")

        then: "Verify the media statistics of MoonCake_1 during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Sending}:--:--:0:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.CVRX, "${expectedProtocol}:${expectedResolution_CVTX}:--:0:--", moonCake_1)

        then: "Stop content and unmute MoonCake video"
        moonCake.stopContent()
        moonCake.muteVideo(false)

        then: "Verify the media statistics of MoonCake after stop content"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")

        then: "Verify the media statistics of MoonCake_1 after stop content"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--", moonCake_1)
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--", moonCake_1)
        logger.info("===============Successfully started SIP Call with call rate " + CallRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        CallRate | expectedProtocol | expectedResolution_PVTX | expectedResolution_CVTX | expectedResolution_PVTX_Sending | expectedResolution_CVRX | expectedResolution_PVTX_Receiving
        1024     | "H.264High"      | "1280x720"              | "1920x1080:384"         | "1280x720"                      | "1920x1080"             | "1280x720"
    }
}
