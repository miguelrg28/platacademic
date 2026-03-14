import { useEffect, useEffectEvent, useRef, useState } from 'react'
import { Html5QrcodeScanType, Html5QrcodeScanner } from 'html5-qrcode'

export function QrScannerPanel({ eventId, busy, onScan, onScanImage }) {
  const [active, setActive] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [scannerMessage, setScannerMessage] = useState('')
  const processingRef = useRef(false)
  const fileInputRef = useRef(null)
  const scannerId = `qr-reader-${eventId}`

  const handleDecode = useEffectEvent(async (decodedText) => {
    if (processingRef.current) {
      return
    }

    processingRef.current = true
    setScannerMessage('Validando QR...')

    try {
      await onScan(decodedText)
      setScannerMessage('Asistencia marcada correctamente.')
      setActive(false)
    } catch (error) {
      setScannerMessage(error.message)
    } finally {
      window.setTimeout(() => {
        processingRef.current = false
      }, 900)
    }
  })

  useEffect(() => {
    if (!active) {
      return undefined
    }

    const scanner = new Html5QrcodeScanner(
      scannerId,
      {
        fps: 10,
        qrbox: {
          width: 230,
          height: 230,
        },
        supportedScanTypes: [Html5QrcodeScanType.SCAN_TYPE_CAMERA],
      },
      false,
    )

    scanner.render(
      (decodedText) => {
        void handleDecode(decodedText)
      },
      () => {},
    )

    return () => {
      scanner.clear().catch(() => {})
    }
  }, [active, scannerId])

  async function handleImageSelection(event) {
    const qrImage = event.target.files?.[0] ?? null
    event.target.value = ''

    if (!qrImage) {
      return
    }

    setUploading(true)
    setScannerMessage('Procesando imagen QR...')

    try {
      await onScanImage(qrImage)
      setScannerMessage('Asistencia marcada correctamente.')
      setActive(false)
    } catch (error) {
      setScannerMessage(error.message)
    } finally {
      setUploading(false)
    }
  }

  return (
    <article className="scanner-card">
      <div className="section-header section-header--compact">
        <div>
          <p className="eyebrow">Control de acceso</p>
          <h3>Escáner QR del evento</h3>
        </div>
        <div className="inline-actions">
          <button
            type="button"
            className="primary-button"
            disabled={busy || uploading}
            onClick={() => {
              setScannerMessage('')
              setActive((current) => !current)
            }}
          >
            {active ? 'Detener cámara' : 'Activar cámara'}
          </button>
          <button
            type="button"
            className="secondary-button"
            disabled={busy || uploading}
            onClick={() => fileInputRef.current?.click()}
          >
            {uploading ? 'Procesando imagen...' : 'Subir imagen QR'}
          </button>
        </div>
      </div>

      <input
        ref={fileInputRef}
        accept="image/png,image/jpeg,image/webp"
        className="visually-hidden"
        type="file"
        onChange={handleImageSelection}
      />

      <div className="scanner-shell">
        {active ? (
          <div id={scannerId} className="scanner-surface" />
        ) : (
          <p>Activa la cámara o sube una imagen con el QR del participante.</p>
        )}
      </div>

      {scannerMessage ? <p className="scanner-message">{scannerMessage}</p> : null}
    </article>
  )
}
