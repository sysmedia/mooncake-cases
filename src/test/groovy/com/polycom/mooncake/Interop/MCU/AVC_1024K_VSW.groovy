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
 * Created by Sophia Song on 2019-04-30.
 *
 * This case is to verify MoonCake calling MCU VSW meeting via VMR with call rate 1024K
 *
 * Environment: MCU register H.323 and SIP to DMA, DMA integrated with MCU for both H.323 and SIP
 *              Conference profile: Line rate 1024k + Encryption auto
 *
 * Check audio, video,content
 * Test with H323 and SIP TCP
 */
class AVC_1024K_VSW extends MoonCakeSystemTestSpec{
    @Shared
    Dma dma

    @Shared
    Mcu mcu

    @Shared
    GroupSeries groupSeries

    @Shared
    GroupSeries groupSeries2

    @Shared
    String vmr = "1024"

    @Shared
    def gs_h323name = "Auto_GS5501"

    @Shared
    def gs_e164 = "1721126554"

    @Shared
    String gs_sip_username = "GS550Sip"

    @Shared
    def gs2_h323name = "Auto_GS700022"

    @Shared
    def gs2_e164 = "17211259922"

    @Shared
    String gs2_sip_username = "GS550Sip2"
    @Shared
    String h323Name = "automooncake1024"

    @Shared
    String e164Num = "843811001024"

    @Shared
    String sipUri = "mooncake1024"

    @Shared
    String expectedResolution_PVRX="1280x720"

    @Shared
    String expectedResolution_PVTX="1280x720"

    def setupSpec() {

        groupSeries = testContext.bookSut(GroupSeries.class, "GS550")
        groupSeries.init()
        groupSeries.setEncryption("no")

        groupSeries2 = testContext.bookSut(GroupSeries.class, "GS700")
        groupSeries2.init()
        groupSeries2.setEncryption("no")

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
    }

    def cleanupSpec() {

        testContext.releaseSut(dma)
        testContext.releaseSut(mcu)
        testContext.releaseSut(groupSeries)

    }


    def "Verify MoonCake Can Join The H323 Conference With Call Rate 4096Kbps by dialing VMR"(String dialString,
                                                                                              int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()
        groupSeries2.hangUp()
        moonCake.enableH323()
        groupSeries.enableH323()
        groupSeries2.enableH323()

        when: "Set the MoonCake and GS with registering the DMA via H323"
        moonCake.registerGk(true, false, dma.ip, h323Name, e164Num, "", "")
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)
        groupSeries2.registerGk(gs2_h323name, gs2_e164, dma.ip)

        then: "Place call on the mooncake with call rate 4096Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.H323, callRate)
            pauseTest(1)
            groupSeries.placeCall(dialString,CallType.H323,callRate)
            pauseTest(1)
            groupSeries2.placeCall(dialString,CallType.H323,callRate)
            pauseTest(1)
        }

        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264:$expectedResolution_PVRX:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264:$expectedResolution_PVTX:--:--:--:--")

        then: "Push content on the GS"
        groupSeries.playHdmiContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264:$expectedResolution_PVRX:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264:$expectedResolution_PVTX:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:1280X720:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        then: "Push content on the RPD"
        groupSeries2.playHdmiContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264:$expectedResolution_PVRX:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264:$expectedResolution_PVTX:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:1280X720:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        then:"Stop play content,verify the media statistics during the call"
        groupSeries2.stopContent()
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264:$expectedResolution_PVRX:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264:$expectedResolution_PVTX:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
        groupSeries2.hangUp()
        moonCake.registerGk(false, false, "", "", "", "", "")
        pauseTest(10)

        where:
        [dialString, callRate] << getTestData_1()
    }


    def "Verify MoonCake Can Join The SIP Conference With Call Rate 1024Kbps by dialing VMR"(String dialString,
                                                                                             String sipTransProtocol,
                                                                                             int callRate) {
        setup:
        moonCake.enableSIP()
        groupSeries.enableSIP()
        groupSeries2.enableSIP()

        when: "Set the MoonCake and GS with registering the SIP"
        moonCake.registerSip("TCP", true, "", dma.ip, "", sipUri, "")
        groupSeries.registerSip(gs_sip_username, centralDomain, "",dma.ip,SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TCP)
        groupSeries2.registerSip(gs_sip_username, centralDomain, "",dma.ip,SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TCP)

        then: "Place call on the MoonCake with call rate 1024Kbps"
        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP, callRate)
            pauseTest(1)
            groupSeries.placeCall(dialString,CallType.SIP,callRate)
            pauseTest(1)
            groupSeries2.placeCall(dialString,CallType.SIP,callRate)
            pauseTest(1)
        }
        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264:$expectedResolution_PVRX:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264:$expectedResolution_PVTX:--:--:--:--")

        then: "Push content on the groupSeries"
        groupSeries.playHdmiContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264:$expectedResolution_PVRX:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264:$expectedResolution_PVTX:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:1280X720:--:--:--:--")
        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")

        then: "Push content on the RPD"
        groupSeries2.playHdmiContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264:$expectedResolution_PVRX:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264:$expectedResolution_PVTX:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:1280X720:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        then:"Stop play content,verify the media statistics during the call"
        groupSeries2.stopContent()
        pauseTest(2)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264:$expectedResolution_PVRX:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264:$expectedResolution_PVTX:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
        groupSeries2.hangUp()
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
        rtn << [vmr, 1024]
        return rtn
    }
    /**
     * Dial String for SIP call
     *
     * @return
     */
    def getTestData_2() {
        def rtn = []
        rtn << [vmr,"TCP", 1024]
        return rtn
    }

}
