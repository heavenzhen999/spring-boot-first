package com.example.demo;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.*;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;
import org.jinterop.dcom.impls.automation.IJIEnumVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA. User: noah Date: 8/16/12 Time: 8:00 AM To change
 * this template use File | Settings | File Templates.
 */
public class WmiService {

    private JIComServer m_ComStub = null;
    private IJIComObject m_ComObject = null;
    private IJIDispatch m_Dispatch = null;
    private String m_Address = null;
    private JISession m_Session = null;
    private IJIDispatch m_WbemServices = null;

    private static final String WMI_CLSID = "76A64158-CB41-11D1-8B02-00600806D9B6";
    private static final String WMI_PROGID = "WbemScripting.SWbemLocator";

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public WmiService(String address) {
        JISystem.setAutoRegisteration(true);
        JISystem.getLogger().setLevel(Level.WARNING);
        m_Address = address;
    }

    public void query(String strQuery) {

        System.out.println("query:" + strQuery);

        JIVariant results[] = new JIVariant[0];
        try {
            results = m_WbemServices.callMethodA("ExecQuery", new Object[] { new JIString(strQuery),
                    JIVariant.OPTIONAL_PARAM(), JIVariant.OPTIONAL_PARAM(), JIVariant.OPTIONAL_PARAM() });
            IJIDispatch wOSd = (IJIDispatch) JIObjectFactory.narrowObject((results[0]).getObjectAsComObject());

            int count = wOSd.get("Count").getObjectAsInt();

            IJIComObject enumComObject = wOSd.get("_NewEnum").getObjectAsComObject();
            IJIEnumVariant enumVariant = (IJIEnumVariant) JIObjectFactory
                    .narrowObject(enumComObject.queryInterface(IJIEnumVariant.IID));

            IJIDispatch wbemObject_dispatch = null;

            for (int c = 0; c < count; c++) {

                Object[] values = enumVariant.next(1);
                JIArray array = (JIArray) values[0];
                Object[] arrayObj = (Object[]) array.getArrayInstance();
                for (int j = 0; j < arrayObj.length; j++) {
                    wbemObject_dispatch = (IJIDispatch) JIObjectFactory
                            .narrowObject(((JIVariant) arrayObj[j]).getObjectAsComObject());
                }

                String str = (wbemObject_dispatch.callMethodA("GetObjectText_", new Object[] { 1 }))[0]
                        .getObjectAsString2();
                System.out.println("(" + c + "):");
                System.out.println(str);
                System.out.println();
            }

        } catch (JIException e) {
            e.printStackTrace();
        }
    }

    public void connect(final String domain, final String username, final String password) {
        try {

            m_Session = JISession.createSession(domain, username, password);
            m_Session.useSessionSecurity(true);
            m_Session.setGlobalSocketTimeout(5000);

            m_ComStub = new JIComServer(JIProgId.valueOf(WMI_PROGID), m_Address, m_Session);

            IJIComObject unknown = m_ComStub.createInstance();
            m_ComObject = unknown.queryInterface(WMI_CLSID);

            m_Dispatch = (IJIDispatch) JIObjectFactory.narrowObject(m_ComObject.queryInterface(IJIDispatch.IID));
            JIVariant results[] = m_Dispatch.callMethodA("ConnectServer",
                    new Object[] { new JIString(m_Address), JIVariant.OPTIONAL_PARAM(), JIVariant.OPTIONAL_PARAM(),
                            JIVariant.OPTIONAL_PARAM(), JIVariant.OPTIONAL_PARAM(), JIVariant.OPTIONAL_PARAM(), 0,
                            JIVariant.OPTIONAL_PARAM() });

            m_WbemServices = (IJIDispatch) JIObjectFactory.narrowObject((results[0]).getObjectAsComObject());

        } catch (JIException e) {
            e.printStackTrace();
            if (m_Session != null) {
                try {
                    JISession.destroySession(m_Session);
                } catch (JIException e1) {
                    logger.error(e.getMessage(), e);
                }
            }
        } catch (UnknownHostException e) {
            if (m_Session != null) {
                try {
                    JISession.destroySession(m_Session);
                } catch (JIException e1) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    public void disconnect() {
        try {
            JISession.destroySession(m_Session);
        } catch (JIException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void main() {
        System.out.println("开始1");
        WmiService wmiService = new WmiService("192.168.2.139");
        try {
            wmiService.connect("root\\cimv2", "asa", "123");
        } catch (Exception e) {
            System.out.println(2);
            //TODO: handle exception
            System.err.println(e);
        }
        // WmiService 
        System.out.println("开始2");
        // 域（一般为空字符），用户名，密码
        // wmiService.connect("", "asa", "");

        // 系统信息
        // wmiService.query("SELECT * FROM Win32_ComputerSystem");

        // // CPU信息
        // wmiService.query("SELECT * FROM Win32_PerfFormattedData_PerfOS_Processor WHERE Name != '_Total'");

        // // 内存信息
        // wmiService.query("SELECT * FROM Win32_PerfFormattedData_PerfOS_Memory");

        // // 磁盘信息
        // wmiService.query("SELECT * FROM Win32_PerfRawData_PerfDisk_PhysicalDisk Where Name != '_Total'");

        // wmiService.disconnect();
    }
}