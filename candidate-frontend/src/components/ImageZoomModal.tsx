import React, { useEffect, useCallback, useRef, useState } from 'react';
import { X, ZoomIn, ZoomOut, RotateCcw } from 'lucide-react';

interface ImageZoomModalProps {
  src: string;
  alt: string;
  isOpen: boolean;
  onClose: () => void;
}

export const ImageZoomModal: React.FC<ImageZoomModalProps> = ({ src, alt, isOpen, onClose }) => {
  const [scale, setScale] = useState(1);
  const overlayRef = useRef<HTMLDivElement>(null);

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    },
    [onClose],
  );

  useEffect(() => {
    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'hidden';
      return () => {
        document.removeEventListener('keydown', handleKeyDown);
        document.body.style.overflow = '';
      };
    }
  }, [isOpen, handleKeyDown]);

  // Reset zoom when modal opens with new image
  useEffect(() => {
    if (isOpen) setScale(1);
  }, [isOpen, src]);

  if (!isOpen) return null;

  const zoomIn = () => setScale((s) => Math.min(s + 0.25, 4));
  const zoomOut = () => setScale((s) => Math.max(s - 0.25, 0.25));
  const resetZoom = () => setScale(1);

  return (
    <div
      ref={overlayRef}
      className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/80 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-label={`Enlarged view: ${alt}`}
      onClick={(e) => {
        if (e.target === overlayRef.current) onClose();
      }}
    >
      {/* Toolbar */}
      <div className="absolute top-4 right-4 flex items-center gap-2 z-10">
        <button
          onClick={zoomIn}
          className="p-2 rounded-full bg-white/20 hover:bg-white/30 text-white transition-colors"
          aria-label="Zoom in"
          title="Zoom in"
        >
          <ZoomIn size={20} />
        </button>
        <button
          onClick={zoomOut}
          className="p-2 rounded-full bg-white/20 hover:bg-white/30 text-white transition-colors"
          aria-label="Zoom out"
          title="Zoom out"
        >
          <ZoomOut size={20} />
        </button>
        <button
          onClick={resetZoom}
          className="p-2 rounded-full bg-white/20 hover:bg-white/30 text-white transition-colors"
          aria-label="Reset zoom"
          title="Reset zoom"
        >
          <RotateCcw size={20} />
        </button>
        <button
          onClick={onClose}
          className="p-2 rounded-full bg-white/20 hover:bg-white/30 text-white transition-colors"
          aria-label="Close"
          title="Close (Esc)"
        >
          <X size={20} />
        </button>
      </div>

      {/* Zoom level indicator */}
      <div className="absolute bottom-4 left-1/2 -translate-x-1/2 text-white/70 text-sm bg-black/40 px-3 py-1 rounded-full">
        {Math.round(scale * 100)}%
      </div>

      {/* Image */}
      <div className="max-w-[90vw] max-h-[85vh] overflow-auto">
        <img
          src={src}
          alt={alt}
          className="block mx-auto transition-transform duration-200 ease-out"
          style={{
            transform: `scale(${scale})`,
            transformOrigin: 'center center',
            maxWidth: scale <= 1 ? '90vw' : 'none',
            maxHeight: scale <= 1 ? '85vh' : 'none',
          }}
          draggable={false}
        />
      </div>
    </div>
  );
};

export default ImageZoomModal;
