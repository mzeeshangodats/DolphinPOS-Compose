# Running Both POS and CFD on the Same Tablet

Yes, it works! You can run both the DolphinPOS app and the Dolphin-CFD app on the same tablet. Here's how:

## How It Works

When both apps run on the same device:
- The POS app runs the WebSocket server on `localhost` (127.0.0.1) or the device's local IP
- The CFD app connects to `localhost` (127.0.0.1) or the same IP address
- Both apps communicate through the device's local network interface

## Setup Options

### Option 1: Use Localhost (Recommended for Single Device)

**In your Dolphin-CFD app**, connect using `127.0.0.1`:

```kotlin
// In CustomerDisplayViewModel or wherever you initialize the client
val client = CustomerDisplayClient(
    gson = Gson(),
    serverIp = "127.0.0.1",  // localhost (IP only, no port in this field)
    serverPort = 8080        // Port is a separate parameter
)
client.connect()
```

**Important**: 
- `serverIp` parameter = IP address only (e.g., "127.0.0.1" or "192.168.1.100")
- `serverPort` parameter = Port number (default is 8080)
- The port is **NOT** included in the IP address string

**In DolphinPOS**, the server will automatically bind to the device's IP, but it also works with localhost.

### Option 2: Use Device's Local IP Address

1. **Get the device's IP address**:
   - Go to Settings → WiFi → Tap on connected network
   - Note the IP address (e.g., `192.168.1.100`)

2. **In DolphinPOS Customer Display Setup**:
   - Enter the device's IP address only (e.g., "192.168.1.100")
   - **Do NOT include the port** - port 8080 is always used automatically

3. **In Dolphin-CFD app**:
   - Use the same IP address to connect
   - Port 8080 is the default (you can omit it or specify explicitly)

```kotlin
val client = CustomerDisplayClient(
    gson = Gson(),
    serverIp = "192.168.1.100",  // IP address only (no port)
    serverPort = 8080             // Port is separate parameter (default is 8080)
)
// Or simply:
val client = CustomerDisplayClient(
    gson = Gson(),
    serverIp = "192.168.1.100"  // Port 8080 is used by default
)
```

## Benefits of Single Device Setup

✅ **Testing**: Perfect for development and testing
✅ **Simplicity**: No need for two separate devices
✅ **Cost-effective**: Use one tablet for both POS and customer display
✅ **Split Screen**: Can run both apps side-by-side on Android tablets

## Split Screen Setup (Android)

If you want to see both apps at the same time:

1. Open DolphinPOS app
2. Open recent apps (swipe up or tap recent button)
3. Long-press on Dolphin-CFD app icon
4. Select "Split screen" or "Open in split screen"
5. Both apps will run side-by-side

## Important Notes

1. **Port Binding**: Make sure port 8080 is not used by another app
2. **Permissions**: Both apps need INTERNET permission
3. **Network**: Even on localhost, Android requires INTERNET permission for local connections
4. **Performance**: Running both apps on one device uses more resources, but modern tablets handle it well

## Testing Checklist

- [ ] DolphinPOS app is running
- [ ] Customer Display is enabled in POS settings
- [ ] Server IP is set (or using localhost)
- [ ] Dolphin-CFD app is running
- [ ] CFD app connects to `127.0.0.1:8080` or device IP
- [ ] Cart updates appear in real-time on CFD app

## Troubleshooting

**Connection fails:**
- Try using `127.0.0.1` instead of device IP
- Check that Customer Display is enabled in POS
- Verify port 8080 is not blocked
- Restart both apps

**No data received:**
- Make sure POS app has items in cart
- Check that WebSocket server started (check POS logs)
- Verify connection state in CFD app

**Apps crash:**
- Check device has enough memory
- Close other heavy apps
- Restart the device if needed

## Code Example for Single Device

```kotlin
// In your Dolphin-CFD ViewModel
class CustomerDisplayViewModel : ViewModel() {
    // For same device, use localhost
    private val client = CustomerDisplayClient(
        gson = Gson(),
        serverIp = "127.0.0.1",  // IP address only (no port in this string)
        serverPort = 8080        // Port is a separate parameter (default is 8080)
    )
    
    // Or simply (port 8080 is default):
    // private val client = CustomerDisplayClient(gson, "127.0.0.1")
    
    init {
        connect()
    }
    
    fun connect() {
        client.connect()
    }
    
    // Observe cart data
    val cartData: StateFlow<CartDisplayData?> = client.cartData
    val connectionState: StateFlow<ConnectionState> = client.connectionState
}
```

## Understanding IP Address and Port

**Important Clarification:**

1. **IP Address Field** (in Customer Display Setup):
   - Enter **ONLY** the IP address: `127.0.0.1` or `192.168.1.100`
   - **DO NOT** include the port (no `:8080`)

2. **Port Number**:
   - Port `8080` is **hardcoded** and always used
   - You don't need to enter it anywhere in the POS app
   - In the CFD app, it's a separate parameter (defaults to 8080)

3. **In CustomerDisplayClient**:
   ```kotlin
   CustomerDisplayClient(
       gson = Gson(),
       serverIp = "127.0.0.1",  // ← IP only (no :8080 here)
       serverPort = 8080        // ← Port is separate parameter
   )
   ```

4. **The WebSocket URL is constructed as**:
   - `ws://127.0.0.1:8080/customer-display` (automatically)
   - You don't need to construct this URL yourself

This setup works perfectly for development, testing, or single-device deployments!

