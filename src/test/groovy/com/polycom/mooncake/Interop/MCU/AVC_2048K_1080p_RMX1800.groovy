package com.polycom.mooncake.Interop.MCU

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.api.rest.plcm_conference_template_v7.EncryptionPolicy
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
 * Created by Sophia Song on 2019-04-29.
 *
 * This case is to verify MoonCake calling MCU via VMR with call rate 2048K
 *
 * Environment: MCU register H.323 and SIP to DMA, DMA integrated with MCU for both H.323 and SIP
 *              Conference profile: Line rate 2048k + Encryption auto
 *
 * Check audio, video,content
 * Test with H323 and SIP TCP
 */
class AVC_2048K_1080p_RMX1800 extends MoonCakeSystemTestSpec{
    @Shared
    Dma dma

    @Shared
    Mcu mcu

    @Shared
    GroupSeries groupSeries

    @Shared
    String vmr = "2056"

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

    def setupSpec() {

        groupSeries = testContext.bookSut(GroupSeries.class, "GS550")
        groupSeries.init()
        groupSeries.setEncryption("no")

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
        try {
            dma.deleteVmr(vmrAVCAuto)
        } catch (Exception e) {
            logger.info("delete vmr auto error")
        }
        try {
            dma.deleteConferenceTemplateByName(callTmplAVCAuto)
        } catch (Exception e) {
            logger.info("delete template auto error")
        }
        dma.createConferenceTemplate(callTmplAVCAuto, "AVC only call template AES Auto", "2048", ConferenceCodecSupport.AVC, EncryptionPolicy.ENCRYPTED_PREFERRED)
        dma.createVmr(vmrAVCAuto, callTmplAVCAuto, poolOrder, dma.domain, dma.username, null, null)
    }
    def cleanupSpec() {
        dma.deleteVmr(vmrAVCAuto)
        dma.deleteConferenceTemplateByName(callTmplAVCAuto)
        testContext.releaseSut(dma)
        testContext.releaseSut(mcu)
        testContext.releaseSut(groupSeries)

    }

    def "Verify MoonCake Can Join The H323 Conference With Call Rate 2048Kbps by dialing VMR#dialString"(String dialString,
                                                                                             int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()
        moonCake.enableH323()
        groupSeries.enableH323()

        when: "Set the mooncake with registering the DMA via H323"
        moonCake.registerGk(true, false, dma.ip, h323Name, e164Num, "", "")
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)
        then: "Place call on the mooncake with call rate 2048Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.H323, callRate)
            groupSeries.placeCall(dialString,CallType.H323,callRate)
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
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264High:1280X720:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        then:"Stop play content,verify the media statistics during the call"
        groupSeries.stopContent()
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")
        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
        moonCake.registerGk(false, false, "", "", "", "", "")
        pauseTest(10)

        where:
        [dialString, callRate] << getTestData_1()
    }

    def "Verify MoonCake Can Join The SIP Conference With Call Rate 2048Kbps by dialing VMR#dialString"(String dialString,
                                                                                            String sipTransProtocol,
                                                                                            int callRate) {
        setup:
        moonCake.enableSIP()
        groupSeries.enableSIP()

        when: "Set the MoonCake with registering the SIP"
        moonCake.registerSip("TCP", true, "", dma.ip, "", sipUri, "")
        groupSeries.registerSip(gs_sip_username, centralDomain, "",dma.ip,SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TCP)

        then: "Place call on the MoonCake with call rate 2048Kbps"
        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP, callRate)
            groupSeries.placeCall(dialString,CallType.SIP,callRate)
            pauseTest(3)
        }
        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")

        then: "Push content on the groupSeries"
        groupSeries.playHdmiContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264High:1280X720:--:--:--:--")
        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")

        then:"Stop play content,verify the media statistics during the call"
        groupSeries.stopContent()
        pauseTest(2)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        //Below resolutions is the current status , maybe needs to change after Ling gets the confirmation from DEV
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")

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
        rtn << [vmrAVCAuto, 2048]
        return rtn
    }
    /**
     * Dial String for SIP call
     *
     * @return
     */
    def getTestData_2() {
        def rtn = []
        rtn << [vmrAVCAuto,"TCP", 2048]
        return rtn
    }

}
