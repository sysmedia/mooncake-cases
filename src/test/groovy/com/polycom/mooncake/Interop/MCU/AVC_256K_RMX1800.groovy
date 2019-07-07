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
import spock.lang.Unroll

/**
 * Created by Gary Wang on 2019-05-15.
 *
 * This case is to verify MoonCake calling MCU via VMR with call rate 256K
 *
 * Environment: MCU register H.323 and SIP to DMA, DMA integrated with MCU for both H.323 and SIP
 *              Conference profile: Line rate 256k + Encryption auto
 *
 * Check audio, video,content
 * Test with H323 encryption on/off  and SIP TCP
 */
class AVC_256K_RMX1800 extends MoonCakeSystemTestSpec{
    @Shared
    Dma dma

    @Shared
    Mcu mcu

    @Shared
    GroupSeries groupSeries

    @Shared
    String vmr = "256"

    @Shared
    def gs_h323name = "Auto_GS550"

    @Shared
    def gs_e164 = "1721126550"

    @Shared
    String gs_sip_username = "GS550Sip"
    @Shared
    String h323Name = "automooncake108"

    @Shared
    String e164Num = "84381108"

    @Shared
    String sipUri = "mooncake256"
    @Shared
    String expectedResolution_PVRX="512x288"
    @Shared
    String expectedResolution_PVTX="640x360"


    def setupSpec() {

        groupSeries = testContext.bookSut(GroupSeries.class, "GS550")
        groupSeries.init()
        groupSeries.setEncryption("no")

        dma = testContext.bookSut(Dma.class, keyword)
        mcu = testContext.bookSut(Mcu.class, keyword)
        def dialString = generateDialString(moonCake)
        sipUri = dialString.sipUri
        h323Name = dialString.h323Name
        e164Num = dialString.e164Number
        def dialString2 = generateDialString(groupSeries)
        gs_h323name = dialString2.h323Name
        gs_e164 = dialString2.e164Number

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

        dma.createConferenceTemplate(callTmplAVCAuto, "AVC only call template AES Auto", "256", ConferenceCodecSupport.AVC, EncryptionPolicy.ENCRYPTED_PREFERRED)
        dma.createVmr(vmrAVCAuto, callTmplAVCAuto, poolOrder, dma.domain, dma.username, null, null)

    }

    def cleanupSpec() {
        dma.deleteVmr(vmrAVCAuto)
        dma.deleteConferenceTemplateByName(callTmplAVCAuto)
        testContext.releaseSut(dma)
        testContext.releaseSut(mcu)
        testContext.releaseSut(groupSeries)
    }
    @Unroll
    def "Verify MoonCake Can Join The H323 Conference With Call Rate 256Kbps with Encryption ON/OFF by dialing VMR#dialString"(String dialString,
                                                                                                                               int callRate,
                                                                                                                               String aesSetting) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()
        moonCake.enableH323()
        moonCake.updateCallSettings(256, aesSetting, true, false, true)
        groupSeries.enableH323()

        when: "Set the mooncake and GS with registering the DMA via H323"
        moonCake.registerGk(true, false, dma.ip, h323Name, e164Num, "", "")
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)

        then: "Place call on the mooncake with call rate 256Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.H323, callRate)
            groupSeries.placeCall(dialString,CallType.H323,callRate)
            pauseTest(3)
        }
        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:512x288:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:$expectedResolution_PVTX:--:--:--:--")

        then: "Push content on the GS"
        groupSeries.playHdmiContent()


        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:352x288:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:$expectedResolution_PVTX:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:1280x720:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        then:"Stop play content,verify the media statistics during the call"
        groupSeries.stopContent()
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:352x288:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:$expectedResolution_PVTX:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
        pauseTest(10)

        where:
        [dialString, callRate,aesSetting] << getTestData_1()

    }
    def "Verify MoonCake Can Join The SIP Conference With Call Rate 256Kbps by dialing VMR#dialString"(String dialString,
                                                                                                       String sipTransProtocol,
                                                                                                       int callRate) {
        setup:
        moonCake.enableSIP()
        groupSeries.enableSIP()
        moonCake.updateCallSettings(256, "off", true, false, true);

        when: "Set the MoonCake and GS with registering the SIP"
        moonCake.registerSip("TCP", true, "", dma.ip, "", sipUri, "")
        groupSeries.registerSip(gs_sip_username, centralDomain, "",dma.ip,SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TCP)

        then: "Place call on the MoonCake with call rate 256Kbps"
        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP, callRate)
            groupSeries.placeCall(dialString,CallType.SIP,callRate)
            pauseTest(3)
        }

        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:512x288:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:$expectedResolution_PVTX:--:--:--:--")

        then: "Push content on the mooncake"
        groupSeries.playHdmiContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:352x288:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:$expectedResolution_PVTX:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:1280x720:--:--:--:--")
        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")

        then:"Stop play content,verify the media statistics during the call"
        groupSeries.stopContent()
        pauseTest(2)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:352x288:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:$expectedResolution_PVTX:--:--:--:--")

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
        rtn << [vmrAVCAuto, 256,"on"]
        rtn << [vmrAVCAuto, 256,"off"]
        return rtn
    }
    /**
     * Dial String for SIP call
     *
     * @return
     */
    def getTestData_2() {
        def rtn = []
        rtn << [vmrAVCAuto,"TCP", 256]
        return rtn
    }
}
