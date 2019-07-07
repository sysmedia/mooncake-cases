package com.polycom.mooncake.Interop.MCU

import com.polycom.auto.resources.enums.SipTransportProtocol
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.Mcu
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared

/**
 * Created by Gary Wang on 2019-05-24.
 *
 * This case is to verify MoonCake calling MCU via VMR with call rate 1024K
 *
 * Environment: MCU register H.323 and SIP to DMA, DMA integrated with MCU for both H.323 and SIP
 *              Conference profile: Line rate 1024K + Encryption auto
 *
 * Check audio, video,content
 * Test with H323 and SIP TCP
 */
class AVC_MP_Bridge_Direct_Call_NonAES extends MoonCakeSystemTestSpec{
    @Shared
    Dma dma

    @Shared
    Mcu mcu

    @Shared
    GroupSeries groupSeries

    @Shared
    String vmr = "10244"

    @Shared
    def gs_h323name = "Auto_GS550"

    @Shared
    def gs_e164 = "1721126888"

    @Shared
    String gs_sip_username = "GS550Sip"
    @Shared
    String h323Name = "automooncake2048"

    @Shared
    String e164Num = "843811002048"

    @Shared
    String sipUri = "mooncake2048"

    @Shared
    String mcuConfNum = "3972"

    @Shared
    String confPasswd = "1234"

    @Shared
    String mcuConfProfileAuto = "SipReg"

    def setupSpec() {

        groupSeries = testContext.bookSut(GroupSeries.class, "GS700")
        groupSeries.init()
        groupSeries.setEncryption("no")

        moonCake.updateCallSettings(1024, "off", true, false, true);

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

        mcu.createConference(mcuConfNum, mcuConfNum, confPasswd, "", mcuConfProfileAuto, "true")
    }

    def cleanupSpec() {

        testContext.releaseSut(dma)
        testContext.releaseSut(mcu)
        testContext.releaseSut(groupSeries)

    }

    def "Verify Mooncake joins VMR by H323, dialing method is mcuIP##ConfID,"(String dialString,
                                                                                             int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()
        moonCake.enableH323()
        groupSeries.enableH323()

        when: "Set the mooncake with registering the DMA via H323"
        moonCake.registerGk(true, false, dma.ip, h323Name, e164Num, "", "")
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)
        then: "Place call on the mooncake with call rate 1024Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        logger.info("dialString is " + dialString)
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.H323, callRate)
            groupSeries.placeCall(dialString,CallType.H323,callRate)
            pauseTest(3)
        }
        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        then: "Push content on the GS"
        groupSeries.playHdmiContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264High:1280x720:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        then:"Mookcake play content,verify the media statistics during the call"
        moonCake.pushContent()
        pauseTest(5)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:1280x720:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
        moonCake.registerGk(false, false, "", "", "", "", "")
        pauseTest(10)

        where:
        [dialString, callRate] << getTestData_1()
    }

    def "Verify MoonCake Can Join The SIP Conference With Call Rate 1024Kbps by dialing ConfID@mcuIP"(String dialString,
                                                                                            String sipTransProtocol,
                                                                                            int callRate) {
        setup:


        moonCake.hangUp()
        groupSeries.hangUp()
         moonCake.enableSIP()
        groupSeries.enableH323()

        when: "Set the MoonCake with registering the SIP"
        moonCake.registerSip("TCP", true, "", dma.ip, "", sipUri, "")
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)

        then: "Place call on the MoonCake with call rate 2048Kbps"
        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP, callRate)
            groupSeries.placeCall( mcu.signalingIPAddress.replaceAll("\"","") + "##" + mcuConfNum,CallType.H323,callRate)
            pauseTest(3)
        }
        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        then: "Push content on the groupSeries"
        groupSeries.playHdmiContent()

        then: "Verify the media statistics during the call"

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264High:1280x720:--:--:--:--")
        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")

        then:"Stop play content,verify the media statistics during the call"
        moonCake.pushContent()
        pauseTest(5)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        //Below resolutions is the current status , maybe needs to change after Ling gets the confirmation from DEV
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:1280x720:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
        moonCake.registerSip(sipTransProtocol, false, "", "", "", "", "")
        pauseTest(3)

        where:
        [dialString, sipTransProtocol, callRate] << getTestData_2()
    }
    def "Verify MoonCake Can Join The SIP Conference by dialing mcuIP,then enter conference ID by DTMF"(String dialString,
                                                                                                      String sipTransProtocol,
                                                                                                      int callRate) {
        setup:


        moonCake.hangUp()
        groupSeries.hangUp()
        moonCake.enableSIP()
        groupSeries.enableSIP()

        when: "Set the MoonCake with registering the SIP"
        moonCake.registerSip("TCP", true, "", dma.ip, "", sipUri, "")
        groupSeries.registerSip(gs_sip_username, centralDomain, "",dma.ip,SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TCP)


        then: "Place call on the MoonCake with call rate 2048Kbps"
        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(mcuPrefix + mcuConfNum, CallType.SIP, callRate)
            moonCake.sendDTMF(confPasswd + "#")
            groupSeries.placeCall(dialString,CallType.SIP,callRate)
            pauseTest(3)
        }

        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        then: "Push content on the groupSeries"
        groupSeries.playHdmiContent()

        then: "Verify the media statistics during the call"

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264High:1280x720:--:--:--:--")
        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")

        then:"Stop play content,verify the media statistics during the call"
        moonCake.pushContent()
        pauseTest(5)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        //Below resolutions is the current status , maybe needs to change after Ling gets the confirmation from DEV
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:1280x720:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
        moonCake.registerSip(sipTransProtocol, false, "", "", "", "", "")
        pauseTest(3)

        where:
        [dialString, sipTransProtocol, callRate] << getTestData_2()
    }


    /**
     * Dial String for H323 call
     *
     * @return
     */
    def getTestData_1() {
        def rtn = []
        String dialString = mcu.signalingIPAddress.replaceAll("\"","") + "##" + mcuConfNum
        logger.info("dialString is " + dialString)
        rtn << [dialString, 1024]
        return rtn
    }
    /**
     * Dial String for SIP call
     *
     * @return
     */
    def getTestData_2() {
        def rtn = []
        String dialString = mcuConfNum + "@" + mcu.signalingIPAddress.replaceAll("\"","")
        rtn << [dialString,"TCP", 1024]
        return rtn
    }

}
