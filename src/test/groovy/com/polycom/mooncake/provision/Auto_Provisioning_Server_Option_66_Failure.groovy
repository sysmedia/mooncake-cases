package com.polycom.mooncake.provision


import com.polycom.honeycomb.ServiceStatus
import com.polycom.honeycomb.ftp.FtpClient
import com.polycom.honeycomb.moonCake.enums.ProvisioningMode
import com.polycom.mooncake.MoonCakeProvisionTestSpec

/**
 * Created by taochen on 2019-06-11.
 *
 * Provisioning mode: DHCP auto_66
 * Provisioning protocl: ftp
 * Provisioning server: ftp
 * DHCP 66 provisioning value is null, MoonCake will not be redirected to ztp.polycom.com for provisioning.
 */
class Auto_Provisioning_Server_Option_66_Failure extends MoonCakeProvisionTestSpec {
    def "Verify if the MoonCake cannot be provisioned by the DHCP option 66 with empty settings"() {
        when: "Create the configuration on the FTP server so that the MoonCake can retrieve the settings"
        FtpClient ftpClient = new FtpClient(FTP_SERVER, FTP_USER, FTP_PASSWORD, "TLS")
        deleteConfigOnFtp(ftpClient, mac)
        createConfigOnFtp(ftpClient, mac)

        then: "Update the DHCH settings for later provisioning"
        doCommandOnHttp(DHCP_SERVER, cmdDel66)
        doCommandOnHttp(DHCP_SERVER, cmdDel160)
        doCommandOnHttp(DHCP_SERVER, cmdFtpSet66WithEmpty)

        then: "MoonCake provision onto the DHCP server to retrieve the settings"
        moonCake.updateProvisioningSettings(ProvisioningMode.AUTO_CUSTOMER, 66, "", "", "", "")

        then: "Verify if the MoonCake retrieve the settings from the DHCP Server"
        retry(times: 3, delay: 5) {
            assert moonCake.gkStatus == ServiceStatus.UNKNOWN
            assert moonCake.sipStatus == ServiceStatus.DISCONNECTED
        }
    }
}
