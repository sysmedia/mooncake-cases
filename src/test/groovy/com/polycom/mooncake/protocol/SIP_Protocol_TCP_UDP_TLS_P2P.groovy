package com.polycom.mooncake.protocol

import com.polycom.api.rest.plcm_sip_identity_v2.SipRegistrationState
import com.polycom.auto.resources.enums.SipTransportProtocol
import com.polycom.honeycomb.*
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.honeycomb.test.logCollecting.CollectSutLogIfFailed
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by qxu on 4/18/2019
 */
@CollectSutLogIfFailed
class SIP_Protocol_TCP_UDP_TLS_P2P extends MoonCakeSystemTestSpec {
    @Shared
    GroupSeries groupSeries

    @Shared
    RpdWin rpdWin

    @Shared
    Dma dma

    @Shared
    String moonSipUri

    @Shared
    String rpdSipUri

    @Shared
    String gsSipUri

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("no")
        groupSeries.enableSIP()
        rpdWin = testContext.bookSut(RpdWin.class, keyword)
        rpdWin.enableSip()
        dma = testContext.bookSut(Dma.class, keyword)
        moonSipUri = generateDialString(moonCake).sipUri
        rpdSipUri = generateDialString(rpdWin).sipUri
        gsSipUri = generateDialString(groupSeries).sipUri
        groupSeries.api().setStereoEnabled(true)
    }

    def cleanupSpec() {
        groupSeries.api().setStereoEnabled(true)
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(rpdWin)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    /*
    P2P SVC call:
    MoonCake (TCP) -> RPD Win (UDP)
    RPD Win (TLS) -> MoonCake (TCP)
    MoonCake (UDP) -> RPD Win (TLS)
    */

    @Unroll
    def "Test P2P SVC call with different SIP transport protocol, moonCake use #moonPcl, RPD use #rpdPcl"(Endpoint caller,
                                                                                                          Endpoint callee,
                                                                                                          int callRate,
                                                                                                          String moonPcl,
                                                                                                          String rpdPcl) {
        setup: "make sure moonCake and RPD was not in call"
        moonCake.hangUp()
        moonCake.updateCallSettings(callRate, "off", true, false, true)
        rpdWin.hangUp()

        when: "register moonCake and Rpd to Sip with special protocol"
        logger.info("Register MoonCake[" + moonPcl + "] Rpd[" + rpdPcl + "]to Sip server")
        moonRpdRegisterSip(moonPcl, rpdPcl)

        then: "Verify if the MoonCake correctly registered on the DMA"
        retry(times: 5, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(moonSipUri)
        }

        then: "start place call"
        logger.info("=====" + caller + " place SVC call to " + callee + " with rate of " + callRate + "=====")
        retry(times: 3, delay: 5) {
            if (callee == moonCake) {
                rpdWin.placeCall(moonSipUri, CallType.SIP, callRate)
                pauseTest(10)
            } else if (callee == rpdWin) {
                moonCake.placeCall(rpdSipUri, CallType.SIP, callRate)
                pauseTest(10)
            }
        }

        then: "push content on Rpd"
        retry(times: 3, delay: 5) {
            rpdWin.pushContent()
        }
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264High:--:--:--:--:--")

        then: "push content on MoonCake"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")

        then: "hang up all call"
        moonCake.hangUp()
        pauseTest(3)
        logger.info("=====Call hang up success!=====")

        where:
        caller   | callee   | callRate | moonPcl | rpdPcl
        moonCake | rpdWin   | 512      | "TCP"   | "UDP"
        rpdWin   | moonCake | 1024     | "TCP"   | "TLS"
        moonCake | rpdWin   | 2048     | "UDP"   | "TLS"
    }

    /*
    P2P AVC call:
    Oculus (TCP) -> GS (UDP)
    GS (TCP) -> Oculus (UDP)
    Oculus (UDP) -> GS (UDP)
    */

    @Unroll
    def "Test P2P AVC call with different SIP transport protocol, moonCake use #moonPcl, GS use #gsPcl"(Endpoint caller,
                                                                                                        Endpoint callee,
                                                                                                        int callRate,
                                                                                                        String moonPcl,
                                                                                                        SipTransportProtocol gsPcl) {
        setup: "make sure moonCake and GS was not in call"
        moonCake.hangUp()
        moonCake.updateCallSettings(callRate, "off", true, false, true)
        groupSeries.hangUp()

        when: "register moonCake and GS to Sip with special protocol"
        logger.info("Register MoonCake[" + moonPcl + "] GroupSeries[" + gsPcl + "]to Sip server")
        moonGsRegisterSip(moonPcl, gsPcl)
        groupSeries.api().setStereoEnabled(false)

        then: "Verify if the MoonCake correctly registered on the DMA"
        retry(times: 5, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(moonSipUri)
        }

        then: "start place call"
        logger.info("=====" + caller + " place AVC call to " + callee + " with rate of " + callRate + "=====")
        retry(times: 3, delay: 5) {
            if (callee == moonCake) {
                groupSeries.placeCall(moonSipUri, CallType.SIP, callRate)
                pauseTest(10)
            } else if (callee == groupSeries) {
                moonCake.placeCall(gsSipUri + "@" + groupSeries.ip, CallType.SIP, callRate)
                pauseTest(10)
            }
        }

        then: "push content on GS"
        groupSeries.playHdmiContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264High:--:--:--:--:--")

        then: "push content on MoonCake"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")

        then: "hang up all call"
        moonCake.hangUp()
        pauseTest(3)
        logger.info("=====Call hang up success!=====")

        where:
        caller      | callee      | callRate | moonPcl | gsPcl
        moonCake    | groupSeries | 512      | "TCP"   | SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_UDP
        groupSeries | moonCake    | 1024     | "UDP"   | SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TCP
        moonCake    | groupSeries | 2048     | "UDP"   | SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_UDP
    }

    //register moonCake and Rpd to dma with special protocol
    def moonRpdRegisterSip(String pcl_moon, String pcl_rpd) {
        moonCake.registerSip(pcl_moon, true, "", dma.getIp(), "", moonSipUri, "")
        pauseTest(5)
        rpdWin.registersip(dma.getIp(), rpdSipUri, "", "", "", pcl_rpd)
        pauseTest(5)
    }

    //register moonCake and GS to dma with special protocol
    def moonGsRegisterSip(String pcl_moon, SipTransportProtocol pcl_gs) {
        moonCake.registerSip(pcl_moon, true, "", dma.ip, "", moonSipUri, "")
        pauseTest(5)
        groupSeries.registerSip(gsSipUri, centralDomain, "", dma.ip, pcl_gs)
        pauseTest(5)
    }
}
