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
 * This case is to verify the matrix testing with RPD Mac by H323 with various call rates
 */

class Interop_Matrix_RPD_Mac_H323 extends MoonCakeSystemTestSpec {
    @Shared
    RpdMac rpdMac

    @Shared
    Dma dma

    @Shared
    String mooncake_e164

    @Shared
    String mooncake_h323Name

    @Shared
    String rpd_e164

    @Shared
    String rpd_h323Name

    def setupSpec() {
        rpdMac = testContext.bookSut(RpdMac.class, keyword)
        rpdMac.init()
        rpdMac.enableH323()
        moonCake.enableH323()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
        def rpdDialString = generateDialString(rpdMac)
        def mcDialString = generateDialString(moonCake)
        rpd_h323Name = rpdDialString.h323Name
        rpd_e164 = rpdDialString.e164Number
        mooncake_h323Name = mcDialString.h323Name
        mooncake_e164 = mcDialString.e164Number
    }

    def cleanupSpec() {
        if (rpdMac != null) {
            testContext.releaseSut(rpdMac)
        }
        testContext.releaseSut(dma)
        moonCake.init()
    }

    @Unroll
    def "Place call from MoonCake to RPD Mac with both H323 registered and call rate #CallRate Kbps"(int CallRate,
                                                                                                     String expectedProtocol,
                                                                                                     String expectedResolution_PVTX,
                                                                                                     String expectedResolution_CVTX,
                                                                                                     String expectedResolution_PVTX_Sending,
                                                                                                     String expectedResolution_CVRX,
                                                                                                     String expectedResolution_PVTX_Receiving) {
        setup:
        moonCake.hangUp()
        rpdMac.hangUp()

        when: " set MoonCake and RPD Mac's call rate"
        moonCake.setCallRate(CallRate)
        rpdMac.setPreferedCallrate(CallRate)


        then: "Set both endpoints H323 registered"
        moonCake.registerGk(true, false, dma.ip, mooncake_h323Name, mooncake_e164, "", "")
        rpdMac.registerH323(dma.ip, rpd_h323Name, rpd_e164)
        pauseTest(3)

        //when: "Collect MoonCake performance data"
        //need API to collect Mooncake performance data

        then: "MoonCake place H323 call to RPD Mac"
        logger.info("===============Start H323 Call with call rate " + CallRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(rpd_e164, CallType.H323, CallRate)
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
        logger.info("===============Successfully started H323 Call with call rate " + CallRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        CallRate | expectedProtocol | expectedResolution_PVTX | expectedResolution_CVTX | expectedResolution_PVTX_Sending | expectedResolution_CVRX | expectedResolution_PVTX_Receiving
        256      | "H.264High"      | "640x360"               | "1280x720:128"          | "320x180"                       | "1280x720"              | "640x360"
        384      | "H.264High"      | "640x360"               | "1280x720:128"          | "640x360"                       | "1280x720"              | "640x360"
        512      | "H.264High"      | "1280x720"              | "1280x720:192"          | "640x360"                       | "1280x720"              | "1280x720"
        1024     | "H.264High"      | "1280x720"              | "1920x1080:384"         | "1280x720"                      | "1920x1080"             | "1280x720"
        1536     | "H.264High"      | "1920x1080"             | "1920x1080:512"         | "1280x720"                      | "1920x1080"             | "1280x720"
        2048     | "H.264High"      | "1920x1080"             | "1920x1080:768"         | "1280x720"                      | "1920x1080"             | "1280x720"
        3072     | "H.264High"      | "1920x1080"             | "1920x1080:1472"        | "1920x1080"                     | "1920x1080"             | "1920x1080"
        4096     | "H.264High"      | "1920x1080"             | "1920x1080:1984"        | "1920x1080"                     | "1920x1080"             | "1920x1080"
    }

    @Unroll
    def "Place call from MoonCake to RPD Mac with both H323 unregistered and call rate 64 Kbps"() {
        setup:
        moonCake.hangUp()
        rpdMac.hangUp()

        when: " set MoonCake and RPD Mac's call rate"
        moonCake.setCallRate(64)
        rpdMac.setPreferedCallrate(64)


        then: "Set both endpoints H323 registered"
        moonCake.registerGk(true, false, dma.ip, mooncake_h323Name, mooncake_e164, "", "")
        rpdMac.registerH323(dma.ip, rpd_h323Name, rpd_e164)
        pauseTest(3)


        then: "MoonCake place H323 call to RPD Mac"
        logger.info("===============Start H323 Call with call rate 64 " + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(rpd_e164, CallType.H323, 64)
        }

        then: "Verify MoonCake's call rate"
        assert moonCake.getCallSettings().call_rate == 64

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        logger.info("===============Successfully started H323 Call with call rate 64 " + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)
    }


    @Unroll
    def "Place call from RPD Mac to MoonCake with both H323 registered and call rate #CallRate Kbps"(int CallRate,
                                                                                                     String expectedProtocol,
                                                                                                     String expectedResolution_PVTX,
                                                                                                     String expectedResolution_CVTX,
                                                                                                     String expectedResolution_PVTX_Sending,
                                                                                                     String expectedResolution_CVRX,
                                                                                                     String expectedResolution_PVTX_Receiving) {
        setup:
        moonCake.hangUp()
        rpdMac.hangUp()

        when: " set MoonCake and RPD Mac's call rate"
        moonCake.setCallRate(CallRate)
        rpdMac.setPreferedCallrate(CallRate)

        then: "Set both MoonCake and RPD Mac to H323 registered"
        moonCake.registerGk(true, false, dma.ip, mooncake_h323Name, mooncake_e164, "", "")
        rpdMac.registerH323(dma.ip, rpd_h323Name, rpd_e164)
        pauseTest(3)

        //when: "Collect MoonCake performance data"
        //need API to collect Mooncake performance data

        then: "MoonCake place H323 call to RPD Mac"
        logger.info("===============Start H323 Call with call rate " + CallRate + "===============")
        retry(times: 3, delay: 5) {
            rpdMac.placeCall(mooncake_e164, CallType.H323, CallRate)
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

        then:"Capture Screen Shot"
        captureScreenShot(moonCake)

        then: "Push content from RPD Mac to MoonCake"
        rpdMac.pushContent()

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedProtocol}:${expectedResolution_PVTX_Sending}:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "${expectedProtocol}:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "${expectedProtocol}:${expectedResolution_CVRX}:--:--:0:--")

        then:"Capture Screen Shot"
        captureScreenShot(moonCake)

        then: "Stop content and unmute video"
        rpdMac.stopContent()
        //need a KPI to unmute RPD video

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
        verifyMediaStatistics(MediaChannelType.PVTX, "${expectedResolution_PVTX_Sending}:--:--:0:--")
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
        logger.info("===============Successfully started H323 Call with call rate " + CallRate + "===============")

        then:"Capture Screen Shot"
        captureScreenShot(moonCake)

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        CallRate | expectedProtocol | expectedResolution_PVTX | expectedResolution_CVTX | expectedResolution_PVTX_Sending | expectedResolution_CVRX | expectedResolution_PVTX_Receiving
        768      | "H.264High"      | "1280x720"              | "1280x720:256"          | "1280x720"                      | "1280x720"              | "1280x720"
        1920     | "H.264High"      | "1920x1080"             | "1920x1080:768"         | "1280x720"                      | "1920x1080"             | "1280x720"
    }

}