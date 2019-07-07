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
class Auto_Provisioning_Server_Option_Custom_160_FTP extends MoonCakeProvisionTestSpec {
    @Shared
    GroupSeries groupSeries

    @Shared
    Dma dma

    @Shared
    String gs_e164Num

    @Shared
    String gs_h323Name

    @Shared
    String mc_e164Num

    @Shared
    String mc_h323Name

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        dma = testContext.bookSut(Dma.class, keyword)
        def gsDialString = generateDialString(groupSeries)
        def mcDialString = generateDialString(moonCake)
        gs_e164Num = gsDialString.e164Number
        gs_h323Name = gsDialString.h323Name
        mc_e164Num = mcDialString.e164Number
        mc_h323Name = mcDialString.h323Name
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
    }

    def "Verify if the MoonCake can be provisioned by the DHCP option 160"() {
        def attributesToBeSet = [provision: [
                'enableSIP'        : 'disable',
                'e164Number'       : mc_e164Num,
                'h323alias'        : mc_h323Name,
                'gatekeeperAddress': dma.ip,
                'h323AuthEnabled'  : 'false',
                'enableH323'       : 'enable']
        ]

        when: "Create the configuration on the FTP server so that the MoonCake can retrieve the settings"
        FtpClient ftpClient = new FtpClient(FTP_SERVER, FTP_USER, FTP_PASSWORD, "TLS")
        deleteConfigOnFtp(ftpClient, mac)
        createConfigOnFtp(ftpClient, mac)

        then: "Update the DHCH settings for later provisioning"
        doCommandOnHttp(DHCP_SERVER, cmdDel66)
        doCommandOnHttp(DHCP_SERVER, cmdDel160)
        doCommandOnHttp(DHCP_SERVER, cmdFtpSet160)

        then: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpClient, mac, attributesToBeSet)
        pauseTest(5)

        then: "MoonCake provision onto the DHCP server to retrieve the settings"
        moonCake.updateProvisioningSettings(ProvisioningMode.AUTO_CUSTOMER, 160, "", "", "", "")

        then: "Verify if the MoonCake retrieve the settings from the DHCP Server"
        retry(times: 3, delay: 5) {
            assert moonCake.getGkStatus() == ServiceStatus.CONNECTED
            assert moonCake.registeredGkServerAddress == dma.ip
        }

        when: "Provision the GS onto the SIP server"
        groupSeries.registerGk(gs_h323Name, gs_e164Num, dma.ip)

        then: "MoonCake place SIP call with the GS"
        moonCake.placeCall(groupSeries, CallType.H323, 2048)

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
