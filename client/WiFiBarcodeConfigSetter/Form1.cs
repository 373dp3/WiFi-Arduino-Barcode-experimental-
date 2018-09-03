using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.IO.Ports;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace WiFiBarcodeConfigSetter
{
    public partial class Form1 : Form
    {
        public Form1()
        {
            InitializeComponent();
            this.textBoxSSID.Text = Properties.Settings.Default.ssid;
            this.textBoxPASSWD.Text = Properties.Settings.Default.password;
            this.textBoxIP.Text = Properties.Settings.Default.ip;
            this.textBoxSubnetMask.Text = Properties.Settings.Default.subnet;
            this.textBoxGateway.Text = Properties.Settings.Default.gateway;
            this.textBoxDNS.Text = Properties.Settings.Default.dns;
            this.textBoxSheetName.Text = Properties.Settings.Default.defaultSheet;
            if (Properties.Settings.Default.server.Length > 0)
            {
                this.textBoxServerIp.Text = Properties.Settings.Default.server;
            }

            if (Properties.Settings.Default.dhcp == 1)
            {
                this.checkBoxDHCP.Checked = true;
            }else
            {
                this.checkBoxDHCP.Checked = false;
            }
        }

        private void buttonUpdate_Click(object sender, EventArgs e)
        {
            var list = SerialPort.GetPortNames();
            this.comboBoxPort.Items.Clear();
            foreach(var port in list)
            {
                this.comboBoxPort.Items.Add(port);
            }
            if (list.Length > 0)
            {
                this.comboBoxPort.SelectedIndex = 0;
            }

        }

        private void checkBoxDHCP_CheckedChanged(object sender, EventArgs e)
        {
            bool isDhcp = this.checkBoxDHCP.Checked;
            this.textBoxIP.Enabled = !isDhcp;
            this.textBoxSubnetMask.Enabled = !isDhcp;
            this.textBoxGateway.Enabled = !isDhcp;
            this.textBoxDNS.Enabled = !isDhcp;
        }

        public class Config
        {
            //{"cmd":"w", "dhcp": 0, "server": "http://192.168.43.35/", "ssid": "i-reporterAP", "pwd": "report2418", "ip": "192.168.70.112", "gw": "192.168.70.50", "mask": "255.255.255.0", "dns": "8.8.8.8", "start": "SEIZOU"}
            #region member
            [JsonProperty("cmd")]
            public string cmd = "w";

            [JsonProperty("dhcp")]
            public int dhcp = 0;

            [JsonProperty("server")]
            public string server = "";

            [JsonProperty("ssid")]
            public string ssid = "";

            [JsonProperty("pwd")]
            public string password = "";

            [JsonProperty("ip")]
            public string ip = "";

            [JsonProperty("gw")]
            public string gateway = "";

            [JsonProperty("mask")]
            public string subnetmask = "";

            [JsonProperty("dns")]
            public string dns = "";

            [JsonProperty("start")]
            public string start = "";

            private Form1 form;
            #endregion

            public Config(Form1 form)
            {
                this.form = form;
                update();
            }

            public void update()
            {
                if (form.checkBoxDHCP.Checked)
                {
                    dhcp = 1;
                    ip = "";
                    subnetmask = "";
                    gateway = "";
                    dns = "";
                }else
                {
                    dhcp = 0;
                    ip = form.textBoxIP.Text;
                    subnetmask = form.textBoxSubnetMask.Text;
                    gateway = form.textBoxGateway.Text;
                    dns = form.textBoxDNS.Text;
                }
                ssid = form.textBoxSSID.Text;
                password = form.textBoxPASSWD.Text;
                start = form.textBoxSheetName.Text;
                server = form.textBoxServerIp.Text;
            }

            public string toJson()
            {
                return JsonConvert.SerializeObject(this);
            }
        }

        private void buttonWrite_Click(object sender, EventArgs e)
        {
            //選択されていない場合の警告処理
            if ((comboBoxPort.SelectedItem == null)
                || (comboBoxPort.SelectedItem.ToString().Length == 0))
            {
                MessageBox.Show("ポートを選択してください");
                return;
            }
            if(this.textBoxServerIp.Text[this.textBoxServerIp.Text.Length-1] != '/')
            {
                this.textBoxServerIp.Text += "/";
            }

            var config = new Config(this);
            wifiConfigJson = config.toJson();
            this.textBoxMessage.Text = wifiConfigJson;
            serial = new SerialPort(this.comboBoxPort.SelectedItem.ToString(), 38400);
            serial.DataReceived += Serial_DataReceived;
            serial.Open();
            threadActive = true;
            pollingThread = new Thread(() => {
                for(int i=0; i<3; i++)
                {
                    if (threadActive == false) { break; }
                    recvMsg = "wait 3s";
                    Invoke(new updateValueDelegate(updateValue));
                    Thread.Sleep(3000);
                    if (threadActive == false) { break; }
                    try
                    {
                        if (serial != null)
                        {
                            serial.WriteLine(wifiConfigJson);
                        }
                    }
                    catch (Exception e2) { break; }
                }
                recvMsg = "thread finishing ...";
                Invoke(new updateValueDelegate(updateValue));
                threadActive = false;
                if (serial != null)
                {
                    serial.Close();
                    serial = null;
                }
            });
            pollingThread.Start();
        }

        private void Serial_DataReceived(object sender, SerialDataReceivedEventArgs e)
        {
            try
            {
                recvMsg = serial.ReadLine();
                Invoke(new updateValueDelegate(updateValue));
            }
            catch (Exception ex) { }
        }

        SerialPort serial = null;
        string wifiConfigJson = "";
        string recvMsg = "";
        Thread pollingThread = null;
        static bool threadActive = false;
        delegate void updateValueDelegate();

        void updateValue()
        {
            if(threadActive==false) { return;  }

            this.textBoxMessage.AppendText("\r\n" + recvMsg);
            if (recvMsg.Contains("write done"))
            {
                threadActive = false;
                this.textBoxMessage.AppendText("\r\n--- 書き込み完了 ---");
            }
            recvMsg = "";

        }


        private void Form1_FormClosing(object sender, FormClosingEventArgs e)
        {
            Properties.Settings.Default.server = this.textBoxServerIp.Text;
            Properties.Settings.Default.ssid = this.textBoxSSID.Text;
            Properties.Settings.Default.password = this.textBoxPASSWD.Text;
            if(this.checkBoxDHCP.Checked)
            {
                Properties.Settings.Default.dhcp = 1;
            }else
            {
                Properties.Settings.Default.dhcp = 0;
            }
            Properties.Settings.Default.ip= this.textBoxIP.Text;
            Properties.Settings.Default.subnet = this.textBoxSubnetMask.Text;
            Properties.Settings.Default.gateway = this.textBoxGateway.Text;
            Properties.Settings.Default.dns = this.textBoxDNS.Text;
            Properties.Settings.Default.defaultSheet = this.textBoxSheetName.Text;
            Properties.Settings.Default.Save();
        }

        private void button1_Click(object sender, EventArgs e)
        {
            if (serial != null)
            {
                serial.Close();
                serial = null;
            }
        }
    }
}
