package com.polycom.mooncake.Interop.DMA

import com.polycom.auto.resources.enums.SipTransportProtocol
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.RpdWin
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

import static com.polycom.honeycomb.test.asserts.PctcAssertions.assertThat

/**
 * Created by Dancy Li on 2019-05-05
 * This case is to verify the matrix testing for MoonCake related functions while it is
 * in P2P DMA gateway call
 */

class Interop_DMA_Gateway_Call extends MoonCakeSystemTestSpec {
    @Shared
    GroupSeries groupSeries

    @Shared
    RpdWin rpdWin

    @Shared
    Dma dma

    @Shared
    String gs_h323name

    @Shared
    String gs_e164

    @Shared
    String gs_sip_username

    @Shared
    String rpdSipUserName

    @Shared
    String rpd_e164

    @Shared
    String rpd_h323Name

    @Shared
    String mk_sipUri

    @Shared
    String mk_h323_e164

    @Shared
    String mk_h323Name

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        rpdWin = testContext.bookSut(RpdWin.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("no")
        moonCake.init()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
        def gsDialString = generateDialString(groupSeries)
        def rpdDialString = generateDialString(rpdWin)
        def mkDialString = generateDialString(moonCake)
        gs_h323name = gsDialString.h323Name
        gs_e164 = gsDialString.e164Number
        gs_sip_username = gsDialString.sipUri
        rpdSipUserName = rpdDialString.sipUri
        rpd_e164 = rpdDialString.e164Number
        rpd_h323Name = rpdDialString.h323Name
        mk_sipUri = mkDialString.sipUri
        mk_h323_e164 = mkDialString.e164Number
        mk_h323Name = mkDialString.h323Name
    }

    def cleanupSpec() {
        if (groupSeries != null) {
            testContext.releaseSut(groupSeries)
        }
        if (rpdWin != null) {
            testContext.releaseSut(rpdWin)
        }
        testContext.releaseSut(dma)
        moonCake.init()
    }

    @Unroll
    def "Verify MoonCake related functions when MoonCake place H323 call to GroupSeries with sipusername and call rate 768 Kbps"() {

        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        when: "GroupSeries register with both H323 and SIP"
        groupSeries.enableH323()
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)
        groupSeries.enableSIP()
        groupSeries.registerSip(gs_sip_username, centralDomain, "", dma.ip, SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TLS)
        pauseTest(3)

        then: "Set MoonCake with H323 registered"
        moonCake.enableH323()
        moonCake.registerGk(true, false, dma.ip, mk_h323Name, mk_h323_e164, "", "")

        then: "MoonCake place H323 call to Group Series"
        logger.info("===============Start H323 Call with call rate " + "768" + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(gs_sip_username, CallType.H323, 768)
        }

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264:--:--:--:--:--")
        //need confirm if should be H.264 or H.264High

        then: "Check GroupSeries has live media channel"
        retry(times: 5, delay: 5) {
            assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.ATX)
            assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.ARX)
            assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.PVRX)
            assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.PVTX)
        }
        logger.info("===============Successfully started H.323 Call with call rate " + "768" + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)
    }

    @Unroll
    def "Verify MoonCake related functions when GroupSeries place SIP call to MoonCake with h323e164 and call rate 512 Kbps"() {

        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        when: "GroupSeries register with both H323 and SIP"
        groupSeries.enableH323()
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)
        groupSeries.enableSIP()
        groupSeries.registerSip(gs_sip_username, centralDomain, "", dma.ip, SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TLS)
        pauseTest(3)

        then: "Set MoonCake with H323 registered"
        moonCake.enableH323()
        moonCake.registerGk(true, false, dma.ip, mk_h323Name, mk_h323_e164, "", "")

        then: "GroupSeries place SIP call to MoonCake"
        logger.info("===============Start SIP Call with call rate " + "512" + "===============")
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(mk_h323_e164, CallType.SIP, 512)
        }

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264:--:--:--:--:--")

        then: "Check GroupSeries has live media channel"
        retry(times: 5, delay: 5) {
            assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.ATX)
            assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.ARX)
            assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.PVRX)
            assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.PVTX)
        }
        logger.info("===============Successfully start H.323 Call with call rate " + "512" + "===============")


        cleanup:
        moonCake.hangUp()
        pauseTest(5)
    }

    @Unroll
    def "Verify MoonCake related functions when GroupSeries place H323 call to MoonCake with sipusername and call rate 1024 Kbps"() {

        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        when: "GroupSeries register with both H323 and SIP"
        groupSeries.enableH323()
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)
        groupSeries.enableSIP()
        groupSeries.registerSip(gs_sip_username, centralDomain, "", dma.ip, SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TLS)
        pauseTest(3)

        then: "Set MoonCake with SIP registered with TLS"
        moonCake.enableSIP()
        moonCake.registerSip("TLS", true, "", dma.ip, "", mk_sipUri, "")

        then: "GroupSeries place SIP call to MoonCake"
        logger.info("===============Start H323 Call with call rate " + "1024" + "===============")
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(mk_sipUri, CallType.H323, 1024)
        }

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264:--:--:--:--:--")

        then: "Check GroupSeries has live media channel"
        retry(times: 5, delay: 5) {
            assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.ATX)
            assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.ARX)
            assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.PVRX)
            assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.PVTX)
        }
        logger.info("===============Successfully started H.323 Call with call rate " + "1024" + "===============")


        cleanup:
        moonCake.hangUp()
        pauseTest(5)
    }

    @Unroll
    def "Verify MoonCake related functions when MoonCake place SIP call to RPDWin with h323e164 and call rate 2048 Kbps"() {

        setup:
        moonCake.hangUp()
        rpdWin.hangUp()
        rpdWin.enableAutoAnswer()

        when: "RPDWin register with both H323 and SIP"
        rpdWin.enableSip()
        rpdWin.registersip(dma.ip, rpdSipUserName, "", "", "", "TLS")
        rpdWin.enableH323()
        rpdWin.registerH323(dma.ip, rpd_h323Name, rpd_e164)
        pauseTest(3)

        then: "Set MoonCake with SIP registered with TLS"
        moonCake.enableSIP()
        moonCake.registerSip("TLS", true, "", dma.ip, "", mk_sipUri, "")

        then: "MoonCake place SIP call to RPDWin"
        logger.info("===============Start SIP Call with call rate " + "2048" + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(rpd_e164, CallType.SIP, 2048)
        }

        then: "Verify the media statistics of MoonCake during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264:--:--:--:--:--")

        then: "Check RPDWin has live media channel"
        retry(times: 5, delay: 5) {
            assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.ATX)
            assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.ARX)
            assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.PVRX)
            assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.PVTX)
        }
        logger.info("===============Successfully started SIP Call with call rate " + "2048" + "===============")


        cleanup:
        moonCake.hangUp()
        rpdWin.hangUp()
        pauseTest(5)
    }

}