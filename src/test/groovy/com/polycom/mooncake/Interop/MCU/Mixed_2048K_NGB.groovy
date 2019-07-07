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

/**
 * Created by Gary Wang on 2019-06-11.
 *
 * This case is to verify MoonCake calling MCU while call rate is 2048K
 * Conference profile: Line rate 2048k + Encryption auto
 *
 * Environment: MCU register H.323 and SIP to DMA, DMA integrated with MCU for both H.323 and SIP
 *              Conference profile: Line rate 2048k + Encryption auto
 *
 * Check audio, video,content
 * Test with H323 and SIP TCP
 */
class Mixed_2048K_NGB extends MoonCakeSystemTestSpec{
    @Shared
    Dma dma

    @Shared
    Mcu mcu

    @Shared
    GroupSeries groupSeries

    @Shared
    RpdWin rpdWin

    @Shared
    String vmr = "204863"

    @Shared
    def gs_h323name, rpd_h323name

    @Shared
    def gs_e164, rpd_e164

    @Shared
    String gs_sip_username, rpd_sip_username

    @Shared
    String h323Name = "automooncake2048"

    @Shared
    String e164Num = "843811002048"

    @Shared
    String sipUri = "mooncake2048"

    def setupSpec() {

        groupSeries = testContext.bookSut(GroupSeries.class, "GS700")
        groupSeries.init()
        groupSeries.setEncryption("no")

        rpdWin = testContext.bookSut(RpdWin.class, "SAT")
        rpdWin.init()

        moonCake = testContext.bookSut(MoonCake.class, keyword)
        moonCake.updateCallSettings(2048, "off", true, false, true);

        dma = testContext.bookSut(Dma.class, keyword)
        mcu = testContext.bookSut(Mcu.class, keyword)
        def dialString = generateDialString(moonCake)
        sipUri = dialString.sipUri
        h323Name = dialString.h323Name
        e164Num = dialString.e164Number

        def dialString2 = generateDialString(groupSeries)
        gs_h323name = dialString2.h323Name
        gs_e164 = dialString2.e164Number
        gs_sip_username = dialString2.sipUri

        def dialString3 = generateDialString(rpdWin)
        rpd_h323name = dialString3.h323Name
        rpd_e164 = dialString3.e164Number
        rpd_sip_username = dialString3.sipUri

        groupSeries.enableH323()
        groupSeries.enableSIP()
        groupSeries.registerGk(gs_h323name,gs_e164,dma.ip)
        groupSeries.registerSip(gs_sip_username, centralDomain, "",dma.ip,SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TCP)

        rpdWin.enableH323()
        rpdWin.enableSip()
        rpdWin.registerH323(dma.ip, rpd_h323name, rpd_e164)
        rpdWin.registersip(dma.ip, rpd_sip_username, centralDomain, "", "", "TCP")

    }

    def cleanupSpec() {

        testContext.releaseSut(dma)
        testContext.releaseSut(mcu)
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(rpdWin)
    }

    def "Verify MoonCake Can Join The H323 Conference With Call Rate 2048Kbps by dialing VMR#dialString"(
            String dialString,  CallType callType1, CallType callType2,  int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()
        rpdWin.hangUp()
        moonCake.enableH323()

        when: "Set the mooncake with registering the DMA via H323"
        moonCake.registerGk(true, false, dma.ip, h323Name, e164Num, "", "")

        then: "Place call on the mooncake with call rate 2048Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            rpdWin.placeCall(dialString, callType1, callRate)
            moonCake.placeCall(dialString, callType2, callRate)
            groupSeries.placeCall(dialString,callType2,callRate)
            pauseTest(3)
        }
        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")
        then: "Push content on the GS"
        groupSeries.playHdmiContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1280X720:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1280X720:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:1280X720:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        then:"MoonCake play content,verify the media statistics during the call"
        moonCake.pushContent()
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1280X720:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1280X720:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:1280X720:--:--:--:--")

        then:"RPDWin play content,verify the media statistics during the call"
        rpdWin.pushContent()
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1280X720:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:1280X720:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
        rpdWin.hangUp()
        moonCake.registerGk(false, false, "", "", "", "", "")
        pauseTest(10)

        where:
        [dialString, callType1, callType2, callRate] << getTestData_1()
    }


    def "Verify MoonCake Can Join The SIP Conference With Call Rate 2048Kbps by dialing VMR#dialString"(
            String dialString,  CallType callType1, CallType callType2,  int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()
        rpdWin.hangUp()
        moonCake.enableSIP()

        when: "Set the mooncake with registering the DMA via H323"
        moonCake.enableSIP()
        pauseTest(3)
        moonCake.registerSip("TCP", true, "", dma.ip, "", sipUri, "")
        pauseTest(10)

        then: "Place call on the mooncake with call rate 2048Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            rpdWin.placeCall(dialString, callType1, callRate)
            moonCake.placeCall(dialString, callType2, callRate)
            groupSeries.placeCall(dialString,callType2,callRate)
            pauseTest(3)
        }
        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")
        then: "Push content on the GS"
        groupSeries.playHdmiContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1280X720:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:1280X720:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        then:"MoonCake play content,verify the media statistics during the call"
        moonCake.pushContent()
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1280X720:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:1280X720:--:--:--:--")

        then:"RPDWin play content,verify the media statistics during the call"
        rpdWin.pushContent()
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1280X720:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:1280X720:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
        rpdWin.hangUp()
        moonCake.registerGk(false, false, "", "", "", "", "")
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
        rtn << [vmr, CallType.SIP, CallType.H323, 2048]
        return rtn
    }
    /**
     * Dial String for SIP call
     *
     * @return
     */
    def getTestData_2() {
        def rtn = []
        rtn << [vmr, CallType.H323, CallType.SIP, 2048]
        return rtn
    }

}
