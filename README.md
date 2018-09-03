# 概要
WiFiマイコン(ESP-WROOM-02)にバーコードリーダー、テンキー、1602液晶を組み合わせたミニマルなシンクライアントです。
生産工程などで大規模導入する場合、パソコン自体のコスト(初期、電気代等のランニングも無視できません。
WiFi Arduino Barcodeでは従来の1/10のコストでの生産工程進捗管理を目指します。

## システム構成(全体)
<img src="https://raw.githubusercontent.com/373dp3/WiFi-Arduino-Barcode-experimental-/master/pict/01kousei.jpg" alt="構成">
<ul>
<li>WiFi Arduino Barcode(HTTP GET/POST & JSON)</li>
<li>ローカルサーバ / Javaを実行可能な環境 / (HTTP経由でのtable参照)</li>
<li>表示用表計算ソフト(LibreOffice等)</li>
</ul>

## ハードウェア構成(クライアント)
<img src="https://raw.githubusercontent.com/373dp3/WiFi-Arduino-Barcode-experimental-/master/pict/02hardware.jpg" alt="構成">
<ul>
<li>ESP-WROOM-02</li>
<li>Mini USB Host Shield(SPI接続)</li>
<li>バーコードリーダー(USB接続)</li>
<li>テンキー(USB接続)</li>
<li>USBハブ</li>
<li>LCD1602(Mini USB Host Shield経由でのGPIO接続)</li>
</ul>

## フォルダ構成
<table>
  <tr>
    <td>client </td>
    <td>ESP-WROOM-02と<a href="https://amzn.to/2LSpzPm">Mini USB Host Shield</a>を用いたバーコードリーダー端末に関する情報
 </td>
  </tr>
  <tr>
    <td>client/eagle </td>
    <td>基板設計ソフト EAGLE用のファイル</td>
  </tr>
  <tr>
    <td>LocalBarcodeServer </td>
    <td>ローカルサーバ用ファイル一式 </td>
  </tr>
  <tr>
    <td>LocalBarcodeServer/binary </td>
    <td>実行に必要なファイルのみ </td>
  </tr>
</table>

