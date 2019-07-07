package com.polycom.mooncake

import com.polycom.mooncake.Interop.FTP.*
import com.polycom.mooncake.provision.*
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Created by taochen on 2019-06-11.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses([
        Auto_Provisioning_Server_Option_66_FTP,
        Auto_Provisioning_Server_Option_66_FTPS,
        Auto_Provisioning_Server_Option_Custom_160_FTP,
        Auto_Provisioning_Server_Option_Custom_160_FTPS,
        Auto_Provisioning_Server_Option_Both_Provisioning,
        Auto_Provisioning_Server_Option_66_FTP_Anonymous,
        Auto_Provisioning_Server_Option_Custom_160_FTP_Anonymous,
        Auto_Provisioning_Server_Option_66_Failure,
        Auto_Provisioning_Server_Option_Custom_160_Failure,
        Auto_Provisioning_Server_Option_66_Server_Switch,
        Auto_Provisioning_Server_Option_Custom_160_Server_Switch,
        Auto_Provisioning_Server_Option_66_To_Custom_Interval,
        Auto_Provisioning_Server_Option_66_To_Custom_Reboot,
        Auto_Provisioning_Server_Option_66_To_Custom_Fail_Invalid_Server,
        Auto_Provisioning_Server_Option_66_To_Custom_Skip_In_A_Call,
        Auto_Provisioning_Server_Option_66_To_Custom_Negative_FTP,
        Auto_Provisioning_Server_Option_Custom_160_To_Custom_Interval,
        Auto_Provisioning_Server_Option_Custom_160_To_Custom_Reboot,
        FTP_Provisioning_No_Mac_CFG_File,
        FTP_Provisioning_General,
        FTP_Provisioning_Call_Settings_Network_Call_Rate_SIP_Callee,
        FTP_Provisioning_Call_Settings_Network_Call_Rate_SIP_Caller,
        FTP_Provisioning_Call_Settings_Network_Call_Rate_H323_Callee,
        FTP_Provisioning_Call_Settings_Network_Call_Rate_H323_Caller,
        FTP_Provisioning_H323,
        FTP_Provisioning_SIP,
        FTP_Provisioning_NTP,
        FTP_Provisioning_Call_Settings_NAT_Config,
        FTP_Provisioning_Call_Settings_AES_Settings_H323,
        FTP_Provisioning_Call_Settings_AES_Settings_SIP,
        FTP_Provisioning_Call_Settings_Network_Fixed_Ports,
        Negative_FTP_scenarios,
        FTP_Provisioning_H323_Authentication
])
class ProvisionTestsSuite {
}
