package bindings

import (
	"io"
	"log"
	"log/slog"
	"os"

	"github.com/google/gopacket"
	"github.com/google/gopacket/layers"
	masqueH2 "github.com/invisv-privacy/masque/http2"
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

type ProxyClient struct {
	*masqueH2.Client
	ProxyIP string
}

func (p *ProxyClient) Close() error {
	return nil
}

func (p *ProxyClient) Connect() error {
	return p.Client.ConnectToProxy()
}

func (p *ProxyClient) CurrentProxyIP() string {
	return p.ProxyIP
}

func (p *ProxyClient) CreateTCPStream(addr string) (io.ReadWriteCloser, error) {
	return p.Client.CreateTCPStream(addr)
}

func (p *ProxyClient) CreateUDPStream(addr string) (io.ReadWriteCloser, error) {
	return p.Client.CreateUDPStream(addr)
}

type Client interface {
	Init() error
	Send(packetData []byte, len int)
}

type client struct {
	pTCP   *pseudotcp.PseudoTCP
	logger *slog.Logger
}

func NewClient(sendPacket PacketSender, protector SocketProtector, proxyFQDN, proxyPort string, verbose bool) Client {
	level := slog.LevelInfo
	if verbose {
		level = slog.LevelDebug
	}
	logger := slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{
		Level: level,
	}))
	slog.SetDefault(logger)

	config := masqueH2.ClientConfig{
		ProxyAddr:  proxyFQDN + ":" + proxyPort,
		IgnoreCert: true,
		Logger:     logger,
		AuthToken:  "fake-token",
		Prot:       masqueH2.SocketProtector(protector.Protect),
	}

	proxyClient := &ProxyClient{
		Client:  masqueH2.NewClient(config),
		ProxyIP: proxyFQDN,
	}

	pTCPConfig := &pseudotcp.PseudoTCPConfig{
		Logger:      logger,
		SendPacket:  pseudotcp.SendPacket(sendPacket.SendPacket),
		ProxyClient: proxyClient,

		// Our test sends to a non-publicly route-able IP
		ProhibitDisallowedIPPorts: false,
	}

	pTCP := pseudotcp.NewPseudoTCP(pTCPConfig)

	pTCP.ConfigureProtect(pseudotcp.SocketProtector(protector.Protect))

	return &client{
		pTCP,
		logger,
	}
}

func (c *client) Init() error {
	return c.pTCP.Init()
}

func (c *client) Send(packetData []byte, len int) {
	c.pTCP.Send(packetData[:len])
}

// PacketInfo is for debugging, prints the given packetData.
func PacketInfo(packetData []byte, len int) {
	// Decode packet - assuming this arrived on a tun interface in Android, so
	// layertype is simply IPv4
	packet := gopacket.NewPacket(packetData, layers.LayerTypeIPv4, gopacket.Default)
	log.Println(packet)
}
