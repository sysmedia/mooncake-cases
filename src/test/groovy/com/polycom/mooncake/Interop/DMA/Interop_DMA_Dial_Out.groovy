package com.polycom.mooncake.Interop.DMA


import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.Mcu
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Gary Wang on 2019-05-15.
 *
 * This case is to verify MoonCake calling MCU via VMR with call rate 1024K
 *
 * Environment: MCU register H.323 and SIP to DMA, DMA integrated with MCU for both H.323 and SIP
 *              Conference profile: Line rate 1024K + Encryption auto
 *
 * Check audio, video,content
 * Test with H323 and SIP TCP
 */
class Interop_DMA_Dial_Out extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    Mcu mcu

    @Shared
    GroupSeries groupSeries

    @Shared
    String vmr = "192001"

    @Shared
    String vmr1 = "192002"

    @Shared
    String vmr2 = "192003"

    @Shared
    String confName = "19200"

    @Shared
    def gs_h323name = "Auto_GS7001"

    @Shared
    def gs_e164 = "172112657001"

    @Shared
    String gs_sip_username = "GS700sSip"
    @Shared
    String h323Name = "automooncake2048"

    @Shared
    String e164Num = "2198108"

    @Shared
    String sipUri = "mooncake192001"

    @Shared
    MoonCake moonCake111

    @Shared
    String confPasswd = "1234#"

    def setupSpec() {

        groupSeries = testContext.bookSut(GroupSeries.class, "GS700")
        groupSeries.init()
        groupSeries.setEncryption("no")

        moonCake.updateCallSettings(1920, "off", true, false, true);

        moonCake111 = testContext.bookSut(MoonCake.class, keyword, "backup")
        moonCake111.updateCallSettings(1920, "off", true, false, true);

        dma = testContext.bookSut(Dma.class, keyword)
        mcu = testContext.bookSut(Mcu.class, keyword)
        def dialString = generateDialString(moonCake)
        h323Name = dialString.h323Name
        def dialString2 = generateDialString(groupSeries)
        gs_h323name = dialString2.h323Name
        gs_e164 = dialString2.e164Number
        gs_sip_username = dialString2.sipUri

        moonCake.enableH323()
        moonCake111.enableSIP()
        groupSeries.enableH323()
        moonCake.registerGk(true, false, dma.ip, h323Name, e164Num, "", "")
        moonCake111.registerSip("TCP", true, "", dma.ip, "", sipUri, "")
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)

    }

    def cleanupSpec() {

        testContext.releaseSut(dma)
        testContext.releaseSut(mcu)
        testContext.releaseSut(groupSeries)

    }

    @Unroll
    def "Verify GS dial into the VMR, and then DMA will automatically dial out to all the mooncakes "(String dialString,
                                                                                                      int callRate) {
        setup:
        moonCake.hangUp()
        moonCake111.hangUp()
        groupSeries.hangUp()

        when: "Set the mooncake with registering the DMA via H323"
        mcu.deleteConferenceByName(confName)
        //pauseTest(60)
        then: "Place call on the mooncake with call rate 2048Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(dialString, CallType.H323, callRate)
            pauseTest(3)
        }
        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1280x720:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1280x720:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1280x720:--:--:--:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1280x720:--:--:--:--", moonCake111)
        then: "Push content on the GS"
        groupSeries.playHdmiContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:1280x720:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:1280x720:--:--:--:--", moonCake111)
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        then: "Stop play content,verify the media statistics during the call"
        groupSeries.stopContent()
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1280X720:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1280X720:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1280X720:--:--:--:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1280X720:--:--:--:--", moonCake111)
        cleanup:
        moonCake.hangUp()
        moonCake111.hangUp()
        groupSeries.hangUp()
        pauseTest(10)

        where:
        [dialString, callRate] << getTestData_1()
    }

    def "Verify GS dial into the VMR with passwd, and then DMA will automatically dial out to all the mooncakes "(String dialString,
                                                                                                                  int callRate) {
        setup:
        moonCake.hangUp()
        moonCake111.hangUp()
        groupSeries.hangUp()

        when: "Set the mooncake with registering the DMA via H323"
        mcu.deleteConferenceByName(confName)
        //pauseTest(60)
        then: "Place call on the mooncake with call rate 2048Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(dialString, CallType.H323, callRate)
            pauseTest(2)
            groupSeries.sendDtmf(confPasswd)
        }
        retry(times: 3, delay: 5) {
            pauseTest(4)
            moonCake.sendDTMF(confPasswd)
            moonCake111.sendDTMF(confPasswd)
        }

        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake111)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake111)


        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")


        cleanup:
        moonCake.hangUp()
        moonCake111.hangUp()
        groupSeries.hangUp()
        pauseTest(10)

        where:
        [dialString, callRate] << getTestData_2()
    }


    /**
     * Dial String for H323 call
     *
     * @return
     */
    def getTestData_1() {
        def rtn = []
        rtn << [vmr, 1920]
        rtn << [vmr1, 1920]
        return rtn
    }
    /**
     * Dial String for SIP call
     *
     * @return
     */
    def getTestData_2() {
        def rtn = []
        rtn << [vmr2, 1920]
        return rtn
    }

}
