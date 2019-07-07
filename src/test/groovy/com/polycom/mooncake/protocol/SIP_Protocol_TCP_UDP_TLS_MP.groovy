package com.polycom.mooncake.protocol

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.api.rest.plcm_sip_identity_v2.SipRegistrationState
import com.polycom.auto.resources.enums.SipTransportProtocol
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.RpdWin
import com.polycom.honeycomb.ServiceStatus
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by qxu on 4/18/2019
 */
class SIP_Protocol_TCP_UDP_TLS_MP extends MoonCakeSystemTestSpec {
    @Shared
    GroupSeries groupSeries

    @Shared
    RpdWin rpdWin

    @Shared
    Dma dma

    @Shared
    String vmrMix = "105490"

    @Shared
    String vmrAVC = "105491"

    @Shared
    String callTmplMix = "mixtmpltest"

    @Shared
    String callTmplAVC = "avctmpltest"

    @Shared
    String sipUri

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("no")
        groupSeries.enableSIP()
        rpdWin = testContext.bookSut(RpdWin.class, keyword)
        rpdWin.enableSip()
        moonCake.updateCallSettings(1024, "off", true, false, true)
        dma = testContext.bookSut(Dma.class, keyword)

        dma.createConferenceTemplate(callTmplMix, "AVC/SVC mixed call template", "2048", ConferenceCodecSupport.MIXED)
        dma.createVmr(vmrMix, callTmplMix, poolOrder, dma.domain, dma.username, null, null)

        dma.createConferenceTemplate(callTmplAVC, "AVC/SVC mixed call template", "2048", ConferenceCodecSupport.AVC)
        dma.createVmr(vmrAVC, callTmplAVC, poolOrder, dma.domain, dma.username, null, null)

        sipUri = generateDialString(moonCake).sipUri
        groupSeries.api().setStereoEnabled(true)
    }

    def cleanupSpec() {
        groupSeries.api().setStereoEnabled(true)
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(rpdWin)
        dma.deleteVmr(vmrMix)
        dma.deleteConferenceTemplateByName(callTmplMix)
        dma.deleteVmr(vmrAVC)
        dma.deleteConferenceTemplateByName(callTmplAVC)
        testContext.releaseSut(dma)
    }

    @Unroll
    def "Test when all EPs join AVC/SVC mixed call with SIP protocol"(String pcl_sut, String pcl_ep1, SipTransportProtocol pcl_ep2) {
        setup: "make sure all EPs were not in call status"
        hangUpAll(moonCake, rpdWin, groupSeries)

        when: "register all EPs"
        logger.info("MoonCale: " + pcl_sut + "; RpdWin:" + pcl_ep1 + "; GS:" + pcl_ep2)
        allEndpointsRegisterSip(pcl_sut, pcl_ep1, pcl_ep2)
        groupSeries.api().setStereoEnabled(false)

        then: "Verify if the MoonCake correctly registered on the DMA"
        retry(times: 5, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(sipUri)
        }

        then: "join vmr call"
        logger.info("=====MoonCake, RpdWin and GroupSeries all join AVC/SVC mixed call=====")
        retry(times: 3, delay: 5) {
            placeCallAll(vmrMix)
        }

        then: "push content on Rpd"
        retry(times: 3, delay: 5) {
            rpdWin.pushContent()
        }
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--")

        then: "push content on MoonCake"
        retry(times: 3, delay: 5) {
            moonCake.pushContent()
        }
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")

        cleanup: "hang up all call"
        hangUpAll(moonCake, rpdWin, groupSeries)
        pauseTest(3)
        logger.info("=====Call hang up success!=====")

        where:
        pcl_sut | pcl_ep1 | pcl_ep2
        "UDP"   | "UDP"   | SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_UDP
        "TCP"   | "TLS"   | SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_UDP
    }

    @Unroll
    def "Test when EPs join AVC only call with SIP protocols"(String pcl_sut, String pcl_ep1, SipTransportProtocol pcl_ep2) {
        setup: "make sure all EPs were not in call status"
        hangUpAll(moonCake, rpdWin, groupSeries)

        when: "register all EPs"
        logger.info("MoonCale: " + pcl_sut + "; RpdWin:" + pcl_ep1 + "; GS:" + pcl_ep2)
        allEndpointsRegisterSip(pcl_sut, pcl_ep1, pcl_ep2)
        groupSeries.api().setStereoEnabled(false)

        then: "Verify if the MoonCake correctly registered on the DMA"
        retry(times: 5, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(sipUri)
        }

        then: "join vmr call"
        logger.info("=====MoonCake, RpdWin and GroupSeries all join AVC only call=====")
        retry(times: 3, delay: 5) {
            placeCallAll(vmrAVC)
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
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--")

        then: "push content on MoonCake"
        retry(times: 3, delay: 5) {
            moonCake.pushContent()
        }
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")

        cleanup: "hang up all call"
        hangUpAll(moonCake, rpdWin, groupSeries)
        pauseTest(3)
        logger.info("=====Call hang up success!=====")

        where:
        pcl_sut | pcl_ep1 | pcl_ep2
        "UDP"   | "TCP"   | SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TLS
        "TCP"   | "TCP"   | SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TCP
    }

    def allEndpointsRegisterSip(String pcl_sut, String pcl_ep1, SipTransportProtocol pcl_ep2) {
        moonCake.registerSip(pcl_sut, true, "", dma.ip, "", sipUri, "")
        rpdWin.registersip(dma.ip, "rpdauto", "", "", "", pcl_ep1)
        groupSeries.registerSip("gsauto", centralDomain, "", dma.ip, pcl_ep2)
        pauseTest(5)
    }

    def placeCallAll(String confVmr) {
        moonCake.placeCall(confVmr, CallType.SIP, 1024)
        rpdWin.placeCall(confVmr, CallType.SIP, 512)
        groupSeries.placeCall(confVmr, CallType.SIP, 2048)
        pauseTest(10)
    }
}