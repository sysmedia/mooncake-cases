package com.polycom.mooncake

import com.polycom.mooncake.WebUITests.AdminSettings.Web_UI_Security_Factory_Restore
import com.polycom.mooncake.WebUITests.AdminSettings.Web_UI_Security_Password
import com.polycom.mooncake.WebUITests.AdminSettings.Web_UI_System_Reboot
import com.polycom.mooncake.WebUITests.AdminSettings.Web_UI_System_Upload_Background
import com.polycom.mooncake.WebUITests.AdminSettings.Web_UI_System_Upload_Background_failure
import com.polycom.mooncake.WebUITests.Contact.Web_UI_Contact_Import_Export
import com.polycom.mooncake.WebUITests.Contact.Web_UI_Contact_Management
import com.polycom.mooncake.WebUITests.Contact.Web_UI_Contact_Search
import com.polycom.mooncake.WebUITests.Diagnostics.Web_UI_Diagnostics_AudioMeter_Test
import com.polycom.mooncake.WebUITests.Diagnostics.Web_UI_Diagnostics_ColorBar_Test
import com.polycom.mooncake.WebUITests.Diagnostics.Web_UI_Diagnostics_Local_Loopback_Test
import com.polycom.mooncake.WebUITests.Diagnostics.Web_UI_Diagnostics_Log
import com.polycom.mooncake.WebUITests.Diagnostics.Web_UI_Diagnostics_Speaker_Test
import com.polycom.mooncake.WebUITests.PlaceCall.Web_UI_PlaceCall
import com.polycom.mooncake.WebUITests.SystemSettings.Web_UI_Network_IP
import com.polycom.mooncake.WebUITests.SystemSettings.Web_UI_System_Call_Setting_Auto_Answer
import com.polycom.mooncake.WebUITests.SystemSettings.Web_UI_System_Call_Setting_Call_Rate
import com.polycom.mooncake.WebUITests.SystemSettings.Web_UI_System_Call_Setting_Other
import com.polycom.mooncake.WebUITests.SystemSettings.Web_UI_System_Date_Time
import com.polycom.mooncake.WebUITests.SystemSettings.Web_UI_System_General
import com.polycom.mooncake.WebUITests.SystemSettings.Web_UI_System_Name
import com.polycom.mooncake.WebUITests.SystemSettings.Web_UI_System_Set_Language
import com.polycom.mooncake.WebUITests.SystemSettings.Web_UI_Vlan_Setting
import com.polycom.mooncake.WebUITests.Web_UI_Help
import com.polycom.mooncake.WebUITests.Web_UI_Login
import com.polycom.mooncake.WebUITests.Web_UI_Set_Web_Language
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite.class)
@Suite.SuiteClasses([
        Web_UI_System_Call_Setting_Auto_Answer,
        Web_UI_System_Call_Setting_Call_Rate,
        Web_UI_System_Call_Setting_Other,
        Web_UI_System_General,
        Web_UI_System_Name,
        Web_UI_System_Set_Language,
        Web_UI_System_Date_Time,
        Web_UI_System_Reboot,
        Web_UI_System_Upload_Background,
        Web_UI_System_Upload_Background_failure,
        Web_UI_Vlan_Setting,
        Web_UI_Network_IP,
        Web_UI_Contact_Search,
        Web_UI_Contact_Import_Export,
        Web_UI_Contact_Management,
        Web_UI_Diagnostics_AudioMeter_Test,
        Web_UI_Diagnostics_ColorBar_Test,
        Web_UI_Diagnostics_Local_Loopback_Test,
        Web_UI_Diagnostics_Log,
        Web_UI_Diagnostics_Speaker_Test,
        Web_UI_Security_Password,
        Web_UI_Security_Factory_Restore,
        Web_UI_Help,
        Web_UI_Set_Web_Language,
        Web_UI_Login,
        Web_UI_PlaceCall
])
class WebUITestsSuite {
}
