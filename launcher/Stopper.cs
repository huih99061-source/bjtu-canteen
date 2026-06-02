using System;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Threading;

class Stopper
{
    const string MYSQL_PORT = "3307";

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
        Console.Title = "北交大食堂仿真系统 - 停止";

        Console.ForegroundColor = ConsoleColor.Yellow;
        Console.WriteLine();
        Console.WriteLine("  +------------------------------------------+");
        Console.WriteLine("  |  北京交通大学食堂仿真系统 - 正在停止      |");
        Console.WriteLine("  +------------------------------------------+");
        Console.ResetColor();
        Console.WriteLine();

        string dir        = ExeDir;
        string mysqladmin = Path.Combine(dir, "mysql", "bin", "mysqladmin.exe");

        Console.Write("  [1/3] 关闭数据库...");
        if (File.Exists(mysqladmin))
        {
            try
            {
                var p = new Process();
                p.StartInfo = new ProcessStartInfo(mysqladmin,
                    "-u root -P" + MYSQL_PORT + " --host=127.0.0.1 --protocol=TCP shutdown")
                { UseShellExecute = false, CreateNoWindow = true };
                p.Start();
                p.WaitForExit(5000);
            }
            catch { }
        }
        Console.ForegroundColor = ConsoleColor.Green;
        Console.WriteLine(" 完成");
        Console.ResetColor();

        Console.Write("  [2/3] 停止后端服务...");
        int killed = 0;
        foreach (Process p in Process.GetProcessesByName("java"))
        {
            try
            {
                string cmd = GetProcessCommandLine(p.Id);
                if (cmd != null && cmd.Contains("app.jar"))
                {
                    p.Kill();
                    killed++;
                }
            }
            catch { }
        }
        Console.ForegroundColor = ConsoleColor.Green;
        Console.WriteLine(killed > 0 ? " 完成（" + killed + " 个进程）" : " 完成（未检测到进程）");
        Console.ResetColor();

        Console.Write("  [3/3] 清理残留进程...");
        try
        {
            foreach (Process p in Process.GetProcessesByName("mysqld"))
                try { p.Kill(); } catch { }
        }
        catch { }
        Thread.Sleep(500);
        Console.ForegroundColor = ConsoleColor.Green;
        Console.WriteLine(" 完成");
        Console.ResetColor();

        Console.WriteLine();
        Console.ForegroundColor = ConsoleColor.Green;
        Console.WriteLine("  ✅ 系统已完全停止。");
        Console.ResetColor();
        Console.WriteLine();
        Console.Write("  按 Enter 键关闭...");
        Console.ReadLine();
    }

    static string GetProcessCommandLine(int pid)
    {
        try
        {
            var p = new Process();
            p.StartInfo = new ProcessStartInfo("wmic",
                "process where processid=" + pid + " get commandline /format:list")
            {
                UseShellExecute        = false,
                CreateNoWindow         = true,
                RedirectStandardOutput = true
            };
            p.Start();
            string output = p.StandardOutput.ReadToEnd();
            p.WaitForExit(2000);
            return output;
        }
        catch
        {
            return null;
        }
    }
}
