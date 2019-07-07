package com.polycom.mooncake.Interop.Endpoints

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.RpdMac
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Dancy Li on 6/6/2019
 * This case is to verify the matrix testing with RPD Mac by SIP with various call rates
 */

class Interop_Matrix_RPD_Mac_SIP extends MoonCakeSystemTestSpec{

    @Shared
    RpdMac rpdMac

    @Shared
    Dma dma

    @Shared
    String sipUri

    @Shared
    String rpd_sip_username

    def setupSpec() {
        rpdMac = testContext.bookSut(RpdMac.class, keyword)
        rpdMac.init()
        rpdMac.enableSip()
        moonCake.enableSIP()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
        sipUri = generateDialString(moonCake).sipUri
        rpd_sip_username = generateDialString(rpdMac).sipUri
    }

    def cleanupSpec() {
        testContext.releaseSut(rpdMac)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    @Unroll
    def "Place call from MoonCake to RPD Mac with both SIP registered and call rate #CallRate Kbps"(int CallRate,
                                                                                                    String expectedProtocol,
                                                                                                    String expectedResolution_PVTX,
                                                                                                    String expectedResolution_CVTX,
                                                                                                    String expectedResolution_PVTX_Sending,
                                                                                                    String expectedResolution_CVRX,
                                                                                                    String expectedResolution_PVTX_Receiving) {
        setup:
        moonCake.hangUp()
        rpdMac.hangUp()

        when: "Set MoonCake call rate"
        moonCake.setCallRate(CallRate)
        rpdMac.setPreferedCallrate(CallRate)

        then: "Set both MoonCake and RPD Mac to SIP registered"
        moonCake.registerSip("TLS", true, "", dma.ip, "", sipUri, "")
        rpdMac.registersip(dma.ip, rpd_sip_username, "", "", "", "TLS")

        //then: "Collect MoonCake performance data"
        //need API to collect Mooncake performance data

        then: "MoonCake place SIP call to RPD Mac"
        logger.info("===============Start SIP Call with call rate " + CallRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(rpd_sip_username, CallType.SIP, CallRate)
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
        verifyMediaStatistics(MediaChannelType.CVTX, "${expectedProtocol}:${expectedResolution_CVTX}:--:--:--:--")

        then:"Capture Screen Shot"
        captureScreenShot(moonCake)

        then: "Stop content and unmute video"
        moonCake.stopContent()
        moonCake.muteVideo(false)

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")

        then:"Capture Screen Shot"
        captureScreenShot(moonCake)

        then: "Push content from RPD Mac to MoonCake"
        rpdMac.pushContent()

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Receiving}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "${expectedProtocol}:${expectedResolution_CVRX}:--:--:0:--")

        then:"Capture Screen Shot"
        captureScreenShot(moonCake)

        then: "Stop content"
        rpdMac.stopContent()

        then: "Verify the media statistics of MoonCake after stop content"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        logger.info("===============Successfully started SIP Call with call rate " + CallRate + "===============")

        then:"Capture Screen Shot"
        captureScreenShot(moonCake)

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        CallRate | expectedProtocol | expectedResolution_PVTX | expectedResolution_CVTX | expectedResolution_PVTX_Sending | expectedResolution_CVRX | expectedResolution_PVTX_Receiving
        256      | "H.264High"      | "640x360"               | "1280x720:128"          | "320x180"                       | "1280x720"              | "640x360"
        512      | "H.264High"      | "1280x720"              | "1280x720:192"          | "640x360"                       | "1280x720"              | "1280x720"
        768      | "H.264High"      | "1280x720"              | "1280x720:256"          | "1280x720"                      | "1280x720"              | "1280x720"
        1536     | "H.264High"      | "1920x1080"             | "1920x1080:512"         | "1280x720"                      | "1920x1080"             | "1280x720"
        2048     | "H.264High"      | "1920x1080"             | "1920x1080:768"         | "1280x720"                      | "1920x1080"             | "1280x720"
        3072     | "H.264High"      | "1920x1080"             | "1920x1080:1472"        | "1920x1080"                     | "1920x1080"             | "1920x1080"
        4096     | "H.264High"      | "1920x1080"             | "1920x1080:1984"        | "1920x1080"                     | "1920x1080"             | "1920x1080"
    }

    @Unroll
    def "Place call from RPD Mac to MoonCake with both SIP registered and call rate 64 Kbps"() {
        setup:
        moonCake.hangUp()
        rpdMac.hangUp()

        when: "Set MoonCake call rate"
        moonCake.setCallRate(64)
        rpdMac.setPreferedCallrate(64)

        then: "Set both MoonCake and RPD Mac to SIP registered"
        moonCake.registerSip("TLS", true, "", dma.ip, "", sipUri, "")
        rpdMac.registersip(dma.ip, rpd_sip_username, "", "", "", "TLS")

        then: "RPD Mac place SIP call to MoonCake"
        logger.info("===============Start SIP Call with call rate 64 " + "===============")
        retry(times: 3, delay: 5) {
            rpdMac.placeCall(sipUri, CallType.SIP, 64)  //audio call only when call rate 64
        }

        then: "verify MoonCake's call rate"
        assert moonCake.getCallSettings().call_rate == 64

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        logger.info("===============Successfully started SIP Call with call rate 64 " + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)
    }

    @Unroll
    def "Place call from RPD Mac to MoonCake with both SIP registered and call rate #CallRate Kbps"(int CallRate,
                                                                                                    String expectedProtocol,
                                                                                                    String expectedResolution_PVTX,
                                                                                                    String expectedResolution_CVTX,
                                                                                                    String expectedResolution_PVTX_Sending,
                                                                                                    String expectedResolution_CVRX,
                                                                                                    String expectedResolution_PVTX_Receiving) {
        setup:
        moonCake.hangUp()
        rpdMac.hangUp()

        when: "Set MoonCake call rate"
        moonCake.setCallRate(CallRate)
        rpdMac.setPreferedCallrate(CallRate)

        then: "Set both MoonCake and RPD Mac to SIP registered"
        moonCake.registerSip("TLS", true, "", dma.ip, "", sipUri, "")
        rpdMac.registersip(dma.ip, rpd_sip_username, "", "", "", "TLS")

        //when: "Collect MoonCake performance data"
        //need API to collect Mooncake performance data

        then: "RPD Mac place SIP call to MoonCake"
        logger.info("===============Start SIP Call with call rate " + CallRate + "===============")
        retry(times: 3, delay: 5) {
            rpdMac.placeCall(sipUri, CallType.SIP, CallRate)
        }

        then: "Verify MoonCake's call rate"
        assert moonCake.getCallSettings().call_rate == CallRate

        then: "Set MoonCake video mute"
        moonCake.muteVideo(true)

        //need a KPI to mute RPD video

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")

        then:"Capture Screen Shot"
        captureScreenShot(moonCake)

        then: "Push content from RPD Mac to MoonCake"
        rpdMac.pushContent()

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Receiving}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "${expectedProtocol}:${expectedResolution_CVRX}:--:--:0:--")

        then:"Capture Screen Shot"
        captureScreenShot(moonCake)

        then: "Stop content and unmute video"
        rpdMac.stopContent()
        moonCake.muteVideo(false)

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")

        then:"Capture Screen Shot"
        captureScreenShot(moonCake)

        then: "Push content from MoonCake to RPD Mac"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Sending}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "${expectedProtocol}:${expectedResolution_CVTX}:--:0:--")

        then:"Capture Screen Shot"
        captureScreenShot(moonCake)

        then: "Stop content"
        moonCake.stopContent()

        then: "Verify the media statistics of MoonCake after stop content"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        logger.info("===============Successfully started SIP Call with call rate " + CallRate + "===============")

        then:"Capture Screen Shot"
        captureScreenShot(moonCake)

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        CallRate | expectedProtocol | expectedResolution_PVTX | expectedResolution_CVTX | expectedResolution_PVTX_Sending | expectedResolution_CVRX | expectedResolution_PVTX_Receiving
        384      | "H.264High"      | "640x360"               | "1280x720:128"          | "640x360"                       | "1280x720"              | "640x360"
        1024     | "H.264High"      | "1280x720"              | "1920x1080:384"         | "1280x720"                      | "1920x1080"             | "1280x720"
        1920     | "H.264High"      | "1920x1080"             | "1920x1080:768"         | "1280x720"                      | "1920x1080"             | "1280x720"
    }

}
