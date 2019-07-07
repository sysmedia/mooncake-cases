package com.polycom.mooncake.Interop.MCU

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared

/**
 * Created by Gary Wang on 2019-05-30.
 *
 #This case is to verify the scenario while MoonCake turn off its video in AVC unencrypted call
 #Far end can see the muted video picture after SUT turn off its video
 #The layout is correct while SUT receives content after it turns off the local video
 #The local video status is correct when SUT turns on/off the video
 */
class Mute_Video_AVC_Unencrypted_Call extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    String vmr = "2048"

    @Shared
    String h323Name1 = "automooncake2048"

    @Shared
    String e164Num1 = "2198108"

    @Shared
    String moonsipUri = "mooncake"

    @Shared
    MoonCake moonCake111

    @Shared
    int callRate = 2048


    def setupSpec() {
        moonCake.updateCallSettings(callRate, "off", true, false, true);
        moonCake111 = testContext.bookSut(MoonCake.class, keyword, "backup")
        moonCake111.updateCallSettings(callRate, "off", true, false, true);
        dma = testContext.bookSut(Dma.class, keyword)
    }

    def cleanupSpec() {
        testContext.releaseSut(dma)
        testContext.releaseSut(moonCake111)
    }

    def "Verify MoonCake joins unencrypted VMR by H323, another MoonCake joins VMR by sip#"() {
        setup:
        moonCake.hangUp()
        moonCake111.hangUp()
        def dialString = generateDialString(moonCake)
        h323Name1 = dialString.h323Name
        e164Num1 = dialString.e164Number
        moonsipUri = dialString.sipUri

        moonCake.enableH323()
        moonCake111.enableSIP()
        pauseTest(5)
        moonCake.registerGk(true, false, dma.ip, h323Name1, e164Num1, "", "")
        moonCake111.registerSip("TCP", true, "", dma.ip, moonsipUri, moonsipUri, "")
        pauseTest(5)
        when: "Place call on the mooncake with call rate 2048Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(vmr, CallType.H323, callRate)
            moonCake111.placeCall(vmr, CallType.SIP, callRate)
            pauseTest(3)
        }
        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--", moonCake111)

        then: "moon cake mute video"
        moonCake.muteVideo(true)


        then: "Push content on the mooncake1"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--", moonCake111)

        then: "Push content on the mooncake2"
        moonCake111.pushContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--", moonCake111)

        then: "mooncake unmute video"
        moonCake.muteVideo(false)

        then: "mooncake stop content"
        moonCake.stopContent()

        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--", moonCake111)

        cleanup:
        moonCake.hangUp()
        moonCake111.hangUp()
        pauseTest(10)

    }

    def "Verify MoonCake joins unencrypted VMR by SIP, another MoonCake joins VMR by H323"() {
        setup:
        moonCake.hangUp()
        moonCake111.hangUp()

        moonCake.enableSIP()
        moonCake111.enableH323()
        pauseTest(5)
        def dialString = generateDialString(moonCake111)
        h323Name1 = dialString.h323Name
        e164Num1 = dialString.e164Number
        moonsipUri = dialString.sipUri

        moonCake111.registerGk(true, false, dma.ip, h323Name1, e164Num1, "", "")
        moonCake.registerSip("TCP", true, "", dma.ip, moonsipUri, moonsipUri, "")
        pauseTest(5)

        when: "Place call on the mooncake with call rate 2048Kbps"
        logger.info("===============Start sip Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake111.placeCall(vmr, CallType.H323, callRate)
            moonCake.placeCall(vmr, CallType.SIP, callRate)
            pauseTest(3)
        }
        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--", moonCake111)

        then: "moon cake mute video"
        moonCake.muteVideo(true)


        then: "Push content on the mooncake1"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--", moonCake111)

        then: "Push content on the mooncake2"
        moonCake111.pushContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "--:--:--:--:1:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.CVTX, "--:--:--:--:--:--", moonCake111)

        then: "mooncake unmute video"
        moonCake.muteVideo(false)

        then: "mooncake stop content"
        moonCake.stopContent()

        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--", moonCake111)

        cleanup:
        moonCake.hangUp()
        moonCake111.hangUp()
        pauseTest(10)

    }


}
