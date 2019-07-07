package com.polycom.mooncake.Interop.MCU

import com.polycom.auto.resources.enums.SipTransportProtocol
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.Mcu
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.RpdWin
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Gary Wang on 2019-06-04.
 *
 *This case is to verify MoonCake and other endpoints join Mixed conference while there is content sharing
 *SUT joins conference and sends content, then FE joins conference
 *
 * Check audio, video,content
 * Test with H323 and SIP TCP
 */
class Mixed_Join_When_Content_Sharing extends MoonCakeSystemTestSpec{
    @Shared
    Dma dma

    @Shared
    Mcu mcu

    @Shared
    RpdWin rpdWin

    @Shared
    String vmr = "409699"

    @Shared
    def rpd_h323name = "Auto_GS550"

    @Shared
    def rpd_e164 = "1721126888"

    @Shared
    String rpd_sip_username = "GS550Sip"
    @Shared
    String h323Name = "automooncake4096"

    @Shared
    String e164Num = "843811004096"

    @Shared
    String sipUri = "mooncake4096"

    def setupSpec() {

        rpdWin = testContext.bookSut(RpdWin.class, keyword)
        rpdWin.init()

        moonCake = testContext.bookSut(MoonCake.class,keyword)
        moonCake.updateCallSettings(4096, "off", true, false, true);

        dma = testContext.bookSut(Dma.class, keyword)
        mcu = testContext.bookSut(Mcu.class, keyword)
        def dialString = generateDialString(moonCake)
        sipUri = dialString.sipUri
        h323Name = dialString.h323Name
        e164Num = dialString.e164Number
        def dialString2 = generateDialString(rpdWin)
        rpd_h323name = dialString2.h323Name
        rpd_e164 = dialString2.e164Number
        rpd_sip_username = dialString2.sipUri

        rpdWin.enableH323()
        rpdWin.enableSip()
        rpdWin.registerH323(dma.ip,rpd_h323name,rpd_e164)
        rpdWin.registersip(dma.ip,rpd_sip_username,"","","","TCP")
    }

    def cleanupSpec() {

        testContext.releaseSut(dma)
        testContext.releaseSut(mcu)
        testContext.releaseSut(rpdWin)

    }

    def "Verify MoonCake joins conference by H.323 and sends content, then RPD join conference" (
            String dialString, CallType callType1, CallType callType2, int callRate) {
        setup:
        moonCake.hangUp()
        rpdWin.hangUp()
        moonCake.enableH323()
        pauseTest(3)

        when: "Set the MoonCake and GS with registering the DMA via H323"
        moonCake.registerGk(true, false, dma.ip, h323Name, e164Num, "", "")
        pauseTest(10)
        then: "Place call on the mooncake with call rate 4096Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.H323, callRate)
            pauseTest(3)
        }

        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")

        then: "Push content on the moonCake"
//        captureScreenShot(moonCake)
        retry(times: 3, delay: 5) {
            moonCake.pushContent()
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:1920x1080:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")
       // captureScreenShot(moonCake)

        then:"RPD join conference ,verify the media statistics during the call"
        retry(times: 3, delay: 5) {
            rpdWin.placeCall(dialString, callType1, callRate)
        }
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:1920x1080:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--", rpdWin)
       // captureScreenShot(moonCake)

        then: "RPD Send content"
        retry(times: 3, delay: 5) {
            rpdWin.pushContent()
        }
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--", rpdWin)
     //   captureScreenShot(moonCake)

        then: "Push content on the moonCake"
        rpdWin.hangUp()
      //  captureScreenShot(moonCake)
        retry(times: 3, delay: 5) {
            moonCake.pushContent()
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")
     //   captureScreenShot(moonCake)

        then:"RPD join conference ,verify the media statistics during the call"
        retry(times: 3, delay: 5) {
            rpdWin.placeCall(dialString, callType2, callRate)
        }
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264SVC:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264SVC:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--", rpdWin)
      //  captureScreenShot(moonCake)

        then: "RPD Send content"
        retry(times: 3, delay: 5) {
            rpdWin.pushContent()
        }
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264SVC:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264SVC:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--", rpdWin)
      //  captureScreenShot(moonCake)


        cleanup:
        moonCake.hangUp()
        rpdWin.hangUp()
        pauseTest(10)

        where:
        [dialString, callType1, callType2, callRate] << getTestData_1()
    }

    def "Verify MoonCake joins conference by SIP and sends content, then RPD join conference" (
            String dialString, CallType callType1, CallType callType2, int callRate) {
        setup:
        moonCake.hangUp()
        rpdWin.hangUp()
        moonCake.enableSIP()
        pauseTest(3)
        when: "Set the MoonCake and GS with registering the DMA via SIP"
        moonCake.registerSip("TCP", true, "", dma.ip, "", sipUri, "")
        pauseTest(15)

        then: "Place call on the mooncake with call rate 4096Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP, callRate)
            pauseTest(3)
        }

        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        then: "Push content on the moonCake"
      //  captureScreenShot(moonCake)
        retry(times: 3, delay: 5) {
            moonCake.pushContent()
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")
        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")
      //  captureScreenShot(moonCake)

        then:"RPD join conference ,verify the media statistics during the call"
        retry(times: 3, delay: 5) {
            rpdWin.placeCall(dialString, callType1, callRate)
        }
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--", rpdWin)
      //  captureScreenShot(moonCake)

        then: "RPD Send content"
        retry(times: 3, delay: 5) {
            rpdWin.pushContent()
        }
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--", rpdWin)
      //  captureScreenShot(moonCake)

        then: "Push content on the moonCake"
        rpdWin.hangUp()
//        captureScreenShot(moonCake)
        retry(times: 3, delay: 5) {
            moonCake.pushContent()
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")
        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")
       // captureScreenShot(moonCake)

        then:"RPD join conference ,verify the media statistics during the call"
        retry(times: 3, delay: 5) {
            rpdWin.placeCall(dialString, callType2, callRate)
        }
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264SVC:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264SVC:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--", rpdWin)
      //  captureScreenShot(moonCake)

        then: "RPD Send content"
        retry(times: 3, delay: 5) {
            rpdWin.pushContent()
        }
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264SVC:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264SVC:--:--:--:--:--", rpdWin)
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--", rpdWin)
      //  captureScreenShot(moonCake)


        cleanup:
        moonCake.hangUp()
        rpdWin.hangUp()
        pauseTest(10)

        where:
        [dialString, callType1, callType2, callRate] << getTestData_2()
    }
      /**
     * Dial String for H323 call
     *
     * @return
     */
    def getTestData_1() {
        def rtn = []
        rtn << [vmr, CallType.H323, CallType.SIP, 4096]
        return rtn
    }
    /**
     * Dial String for SIP call
     *
     * @return
     */
    def getTestData_2() {
        def rtn = []
        rtn << [vmr, CallType.H323, CallType.SIP, 4096]
        return rtn
    }

}
