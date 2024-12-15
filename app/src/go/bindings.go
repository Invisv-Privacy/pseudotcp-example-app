package bindings

import (
	"log"

	"github.com/google/gopacket"
	"github.com/google/gopacket/layers"
	"github.com/invisv-privacy/pseudotcp"
)

// SocketProtector provides the interface that will override
// VpnService.protect() in Android.
type SocketProtector interface {
	Protect(fileDescriptor int) error
}

type PacketSender interface {
	SendPacket(packet []byte, length int) error
}

// ProtectConnections should be called first; allows connections to be protected from
// routing via a VPN, avoiding an infinite routing loop.
func ProtectConnections(dnsServer string, protector SocketProtector) {
	pseudotcp.ConfigureProtect(protector.Protect)
}

// Init should only be called after ProtectConnections.
func Init(pktSender PacketSender, verbose bool, proxyFQDN, proxyPort string) error {
	return pseudotcp.Init(pktSender.SendPacket, verbose, proxyFQDN, proxyPort)
}

// Reconnect should be only called when the vpn is active but possibly in need of reconnection.
func Reconnect(proxyFQDN, proxyPort string) {
	pseudotcp.ReconnectToProxy(proxyFQDN, proxyPort)
}

// Send sends a packet out through the vpn.
func Send(packetData []byte, len int) {
	pseudotcp.Send(packetData[:len])
}

// PacketInfo is for debugging, prints the given packetData.
func PacketInfo(packetData []byte, len int) {
	// Decode packet - assuming this arrived on a tun interface in Android, so
	// layertype is simply IPv4
	packet := gopacket.NewPacket(packetData, layers.LayerTypeIPv4, gopacket.Default)
	log.Println(packet)
}
