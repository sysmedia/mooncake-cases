package com.polycom.mooncake.provision

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.ServiceStatus
import com.polycom.honeycomb.ftp.FtpClient
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.honeycomb.moonCake.enums.ProvisioningMode
import com.polycom.mooncake.MoonCakeProvisionTestSpec
import spock.lang.Shared

/**
 * Created by taochen on 2019-06-10.
 */
class Auto_Provisioning_Server_Option_Custom_160_FTPS extends MoonCakeProvisionTestSpec {
    @Shared
    GroupSeries groupSeries

    @Shared
    Dma dma

    @Shared
    String gs_sip_username

    @Shared
    String mc_sip_username

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        dma = testContext.bookSut(Dma.class, keyword)
        gs_sip_username = generateDialString(groupSeries).sipUri
        mc_sip_username = generateDialString(moonCake).sipUri
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
    }

    def "Verify if the MoonCake can be provisioned by the DHCP option 160"() {
        def attributesToBeSet = [provision: [
                'enableSIP'         : 'enable',
                'sipProxyServer'    : dma.ip,
                'sipRegistrarServer': dma.ip,
                'userName'          : mc_sip_username,
                'transport'         : 'TCP',
                'enableH323'        : 'disable']
        ]

        when: "Create the configuration on the FTP server so that the MoonCake can retrieve the settings"
        FtpClient ftpClient = new FtpClient(FTP_SERVER, FTPS_USER, FTPS_PASSWORD, "TLS")
        deleteConfigOnFtp(ftpClient, mac)
        createConfigOnFtp(ftpClient, mac)

        then: "Update the DHCH settings for later provisioning"
        doCommandOnHttp(DHCP_SERVER, cmdDel66)
        doCommandOnHttp(DHCP_SERVER, cmdDel160)
        doCommandOnHttp(DHCP_SERVER, cmdFtpsSet160)

        then: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpClient, mac, attributesToBeSet)
        pauseTest(5)

        then: "MoonCake provision onto the DHCP server to retrieve the settings"
        moonCake.updateProvisioningSettings(ProvisioningMode.AUTO_CUSTOMER, 160, "", "", "", "")

        then: "Verify if the MoonCake retrieve the settings from the DHCP Server"
        retry(times: 3, delay: 5) {
            assert moonCake.getSipStatus() == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
        }

        when: "Provision the GS onto the SIP server"
        groupSeries.registerSip(gs_sip_username, centralDomain, "", dma.ip)

        then: "MoonCake place SIP call with the GS"
        moonCake.placeCall(groupSeries, CallType.SIP, 2048)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
    }
}
