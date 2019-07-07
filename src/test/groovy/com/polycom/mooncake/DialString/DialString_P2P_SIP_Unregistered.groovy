package com.polycom.mooncake.DialString

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.honeycomb.test.logCollecting.CollectSutLogIfFailed
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Dancy Li on 2019-04-12.
 * Test different dial string when SIP not registered, standalone mode, SUT will do dial string resolve
 * Check audio, video, content sending and receiving.
 * Disable DHCP
 * Test dial out to not registered EP
 * Test dial out to registered EP
 * Test UDP and TCP
 */
@CollectSutLogIfFailed
class DialString_P2P_SIP_Unregistered extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    GroupSeries groupSeries

    @Shared
    String gs_sip_username

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("no")
        groupSeries.enableSIP()
        moonCake.enableSIP()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
        gs_sip_username = generateDialString(groupSeries).sipUri
    }

    def cleanupSpec() {
        groupSeries.api().startReboot() //make sure the GS cannot crash in the later tests
        pauseTest(360)
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    @Unroll
    def "Verify SIP P2P call without registration with dial string #dialString and call rate #callRate Kbps and transport #sipTransProtocol when remote endpoint is SIP not registered"(String dialString,
                                                                                                                                                                                        String sipTransProtocol,
                                                                                                                                                                                        int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        when: "Set mooncake to SIP not registered"
        moonCake.registerSip(sipTransProtocol, false, "", "", "", "", "")

        then: "MoonCake place call to the GroupSeries with various dial string and call rate"
        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP, callRate)
            pauseTest(10)
        }

        then: "Push content on the mooncake"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")
        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        [dialString, sipTransProtocol, callRate] << getTestData_1()
    }

    @Unroll
    def "Verify SIP P2P call without registration with dial string #dialString and call rate #callRate Kbps and transport #sipTransProtocol when remote endpoint is SIP registered"(String dialString,
                                                                                                                                                                                    String sipTransProtocol,
                                                                                                                                                                                    int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()


        when: "Set Mooncake to SIP not registered and Group Series SIP registered"
        moonCake.registerSip(sipTransProtocol, false, " ", "", "", "", "")
        groupSeries.registerSip(gs_sip_username, centralDomain, "", dma.ip)

        then: "MoonCake place call to the GroupSeries with various dial string and call rate"
        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP, callRate)
            pauseTest(10)
        }

        then: "Push content on the mooncake"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")
        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        [dialString, sipTransProtocol, callRate] << getTestData_2()
    }
    /** Create a list of different dial string:
     * IP:Port, FQDN, FQDN:port, sip:IP, sip:FQDN,
     * Hostname:port, sip:Hostname
     *
     * @return
     */
    def getTestData_1() {
        def rtn = []
        def fe_fqdn = groupSeries.hostName + "." + groupSeries.domainName
        callRateList.each {
            rtn << [groupSeries.ip + ":5060", "TCP", it]
            rtn << [groupSeries.ip + ":5060", "UDP", it]
            rtn << [fe_fqdn, "TCP", it]
            rtn << [fe_fqdn, "UDP", it]
            rtn << [fe_fqdn + ":5060", "TCP", it]
            rtn << [fe_fqdn + ":5060", "UDP", it]
            rtn << ["sip:" + groupSeries.ip, "TCP", it]
            rtn << ["sip:" + groupSeries.ip, "UDP", it]
            rtn << ["sip:" + fe_fqdn, "TCP", it]
            rtn << ["sip:" + fe_fqdn, "UDP", it]
            rtn << [groupSeries.hostName + ":5060", "TCP", it]
            rtn << [groupSeries.hostName + ":5060", "UDP", it]
        }
        return rtn
    }

    /** Create a list of different dial string:
     * SIPURI@FE_IP, SIPURI@DMA_IP, SIPURI@FE_FQDN,
     * SIPURI@DMA_Domain, sip:SIPURI@DMA_FQDN, sip:SIPURI@DMA_IP
     */

    def getTestData_2() {
        def rtn = []
        def fe_fqdn = groupSeries.getHostName() + "." + groupSeries.getDomainName()
        def dma_domain = dma.networkSettings.dnsDomain
        def dma_fqdn = dma.networkSettings.physicalHostName + "." + dma.networkSettings.dnsDomain
        callRateList.each {
            rtn << [gs_sip_username + "@" + groupSeries.getIp(), "TCP", it]
            rtn << [gs_sip_username + "@" + groupSeries.getIp(), "UDP", it]
            rtn << [gs_sip_username + "@" + dma.ip, "TCP", it]
            rtn << [gs_sip_username + "@" + dma.ip, "UDP", it]
            rtn << [gs_sip_username + "@" + fe_fqdn, "TCP", it]
            rtn << [gs_sip_username + "@" + fe_fqdn, "UDP", it]
            rtn << [gs_sip_username + "@" + dma_domain, "TCP", it]
            rtn << [gs_sip_username + "@" + dma_domain, "UDP", it]
            rtn << ["sip:" + gs_sip_username + "@" + dma.ip, "TCP", it]
            rtn << ["sip:" + gs_sip_username + "@" + dma.ip, "UDP", it]
            rtn << ["sip:" + gs_sip_username + "@" + dma_fqdn, "TCP", it]
            rtn << ["sip:" + gs_sip_username + "@" + dma_fqdn, "UDP", it]
        }
        return rtn
    }
}
