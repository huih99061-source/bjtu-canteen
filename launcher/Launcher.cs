using System;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Text;
using System.Threading;

class Launcher
{
    const string MYSQL_PORT   = "3307";
    const string BACKEND_PORT = "8888";

    static string ExeDir
    {
        get
        {
            string loc = System.Reflection.Assembly.GetExecutingAssembly().Location;
            return Path.GetDirectoryName(loc);
        }
    }

    static void Main()
    {
        Console.OutputEncoding = Encoding.UTF8;
        Console.Title = "北交大食堂仿真系统 - 启动中";
        PrintBanner();

        string dir       = ExeDir;
        string javaExe   = Path.Combine(dir, "jre",   "bin", "java.exe");
        string mysqldExe = Path.Combine(dir, "mysql", "bin", "mysqld.exe");

        if (!File.Exists(javaExe))
        {
            Err("[错误] 未找到 jre\\bin\\java.exe，请确保压缩包已完整解压。");
            return;
        }
        if (!File.Exists(mysqldExe))
        {
            Err("[错误] 未找到 mysql\\bin\\mysqld.exe，请确保压缩包已完整解压。");
            return;
        }

        string mysqlBin  = Path.Combine(dir, "mysql", "bin");
        string mysqlIni  = Path.Combine(dir, "mysql", "my.ini");
        string mysqlBase = Path.Combine(dir, "mysql");
        string mysqlData = Path.Combine(dir, "mysql", "data");

        if (!Directory.Exists(mysqlData))
        {
            Info("[初始化] 首次运行，正在初始化数据库，请稍候（约30秒）...");

            RunHidden(mysqldExe,
                "--defaults-file=\"" + mysqlIni + "\" " +
                "--basedir=\"" + mysqlBase + "\" " +
                "--datadir=\"" + mysqlData + "\" " +
                "--initialize-insecure --console",
                waitForExit: true);

            Info("[初始化] 启动数据库服务...");
            StartMySQL(mysqlBin, mysqlIni, mysqlBase, mysqlData);

            Info("[初始化] 等待数据库就绪...");
            WaitForMySQL(mysqlBin, 30);

            Info("[初始化] 导入初始数据...");
            string sqlFile = Path.Combine(dir, "init.sql");
            var imp = new Process();
            imp.StartInfo = new ProcessStartInfo(
                Path.Combine(mysqlBin, "mysql.exe"),
                "-u root -P" + MYSQL_PORT + " --host=127.0.0.1 --protocol=TCP")
            {
                UseShellExecute       = false,
                CreateNoWindow        = true,
                RedirectStandardInput = true
            };
            imp.Start();
            imp.StandardInput.Write(File.ReadAllText(sqlFile, Encoding.UTF8));
            imp.StandardInput.Close();
            imp.WaitForExit();
            OK("[初始化] 数据库初始化完成！");
            Console.WriteLine();
        }
        else
        {
            Info("[1/3] 启动数据库服务...");
            StartMySQL(mysqlBin, mysqlIni, mysqlBase, mysqlData);
            WaitForMySQL(mysqlBin, 15);
        }

        Info("[2/3] 启动后端应用...");
        StartBackend(dir);

        Info("[3/3] 等待后端就绪...");
        bool ready = WaitForBackend(35);
        Console.WriteLine();

        if (ready)
            OK("✅ 后端已就绪，使用完整仿真模式");
        else
            Warn("⚠  后端启动超时，将以前端本地仿真模式运行");

        CreateShortcut(dir);

        Info("[完成] 正在打开浏览器...");
        string frontend = Path.Combine(dir, "frontend", "index.html");
        RunHidden("cmd", "/c start \"\" \"" + frontend + "\"", waitForExit: false);

        Console.WriteLine();
        Console.ForegroundColor = ConsoleColor.Cyan;
        Console.WriteLine("  +------------------------------------------+");
        Console.WriteLine("  |  系统已成功启动！                        |");
        Console.WriteLine("  |                                          |");
        Console.WriteLine("  |  浏览器将自动打开食堂仿真页面            |");
        Console.WriteLine("  |  桌面已创建快捷方式：北交大食堂仿真      |");
        Console.WriteLine("  |                                          |");
        Console.WriteLine("  |  停止系统：双击  停止系统.exe            |");
        Console.WriteLine("  +------------------------------------------+");
        Console.ResetColor();
        Console.WriteLine();
        Console.Write("  按 Enter 键关闭此窗口（系统继续后台运行）...");
        Console.ReadLine();
    }

    static void StartMySQL(string bin, string ini, string baseDir, string data)
    {
        var p = new Process();
        p.StartInfo = new ProcessStartInfo(
            Path.Combine(bin, "mysqld.exe"),
            "--defaults-file=\"" + ini + "\" --basedir=\"" + baseDir + "\" --datadir=\"" + data + "\"")
        { UseShellExecute = false, CreateNoWindow = true };
        p.Start();
    }

    static void WaitForMySQL(string mysqlBin, int timeoutSec)
    {
        string admin = Path.Combine(mysqlBin, "mysqladmin.exe");
        for (int i = 0; i < timeoutSec; i++)
        {
            Thread.Sleep(1000);
            try
            {
                var p = new Process();
                p.StartInfo = new ProcessStartInfo(admin,
                    "-u root -P" + MYSQL_PORT + " --host=127.0.0.1 --protocol=TCP --connect-timeout=2 ping")
                { UseShellExecute = false, CreateNoWindow = true, RedirectStandardOutput = true };
                p.Start();
                string output = p.StandardOutput.ReadToEnd();
                p.WaitForExit();
                if (output.Contains("alive"))
                {
                    Console.Write("\r     数据库已就绪（" + (i + 1) + "秒）           \n");
                    return;
                }
            }
            catch { }
            Console.Write("\r     等待数据库... " + (i + 1) + "/" + timeoutSec + " 秒");
        }
        Console.WriteLine();
    }

    static void StartBackend(string dir)
    {
        string java = Path.Combine(dir, "jre", "bin", "java.exe");
        string jar  = Path.Combine(dir, "app.jar");
        string url  = "jdbc:mysql://127.0.0.1:" + MYSQL_PORT + "/bjtu_canteen" +
                      "?useSSL=false&serverTimezone=Asia/Shanghai" +
                      "&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
        var p = new Process();
        p.StartInfo = new ProcessStartInfo(java,
            "-jar \"" + jar + "\" " +
            "\"--spring.datasource.url=" + url + "\" " +
            "--spring.datasource.username=root " +
            "--spring.datasource.password=")
        {
            UseShellExecute  = false,
            CreateNoWindow   = true,
            WorkingDirectory = dir
        };
        p.Start();
    }

    static bool WaitForBackend(int timeoutSec)
    {
        string url = "http://localhost:" + BACKEND_PORT + "/api/sim/snapshot";
        for (int i = 0; i < timeoutSec; i++)
        {
            Thread.Sleep(1000);
            try
            {
                var req = (HttpWebRequest)WebRequest.Create(url);
                req.Timeout = 1500;
                using (var resp = (HttpWebResponse)req.GetResponse())
                {
                    if ((int)resp.StatusCode == 200)
                    {
                        Console.Write("\r     后端已就绪（" + (i + 1) + "秒）                  \n");
                        return true;
                    }
                }
            }
            catch { }
            Console.Write("\r     等待后端服务... " + (i + 1) + "/" + timeoutSec + " 秒");
        }
        return false;
    }

    static void CreateShortcut(string dir)
    {
        try
        {
            string desktop      = Environment.GetFolderPath(Environment.SpecialFolder.Desktop);
            string shortcutPath = Path.Combine(desktop, "北交大食堂仿真.lnk");
            if (File.Exists(shortcutPath)) return;

            string target = Path.Combine(dir, "启动系统.exe");
            Encoding enc = Encoding.GetEncoding(936); // GBK for VBScript on Chinese Windows
            string vbs =
                "Set ws = CreateObject(\"WScript.Shell\")\r\n" +
                "Set sc = ws.CreateShortcut(\"" + shortcutPath + "\")\r\n" +
                "sc.TargetPath = \"" + target + "\"\r\n" +
                "sc.WorkingDirectory = \"" + dir + "\"\r\n" +
                "sc.Description = \"北京交通大学食堂仿真系统\"\r\n" +
                "sc.Save\r\n";

            string tmp = Path.Combine(Path.GetTempPath(), "bjtu_shortcut.vbs");
            File.WriteAllText(tmp, vbs, enc);
            RunHidden("cscript", "//nologo \"" + tmp + "\"", waitForExit: true);
            try { File.Delete(tmp); } catch { }
            OK("     桌面快捷方式已创建：北交大食堂仿真");
        }
        catch (Exception ex)
        {
            Warn("     [警告] 快捷方式创建失败: " + ex.Message);
        }
    }

    static void RunHidden(string exe, string args, bool waitForExit)
    {
        var p = new Process();
        p.StartInfo = new ProcessStartInfo(exe, args)
        { UseShellExecute = false, CreateNoWindow = true };
        p.Start();
        if (waitForExit) p.WaitForExit();
    }

    static void PrintBanner()
    {
        Console.ForegroundColor = ConsoleColor.Red;
        Console.WriteLine();
        Console.WriteLine("  =====================================");
        Console.WriteLine("   北京交通大学  学生活动中心食堂仿真");
        Console.WriteLine("   Simulation System v1.0  [Portable]");
        Console.WriteLine("  =====================================");
        Console.ResetColor();
        Console.WriteLine();
    }

    static void Info(string msg)
    {
        Console.ForegroundColor = ConsoleColor.Cyan;
        Console.WriteLine("  " + msg);
        Console.ResetColor();
    }
    static void OK(string msg)
    {
        Console.ForegroundColor = ConsoleColor.Green;
        Console.WriteLine("  " + msg);
        Console.ResetColor();
    }
    static void Warn(string msg)
    {
        Console.ForegroundColor = ConsoleColor.Yellow;
        Console.WriteLine("  " + msg);
        Console.ResetColor();
    }
    static void Err(string msg)
    {
        Console.ForegroundColor = ConsoleColor.Red;
        Console.WriteLine("  " + msg);
        Console.ResetColor();
        Console.Write("  按 Enter 键退出...");
        Console.ReadLine();
        Environment.Exit(1);
    }
}
