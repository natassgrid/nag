// src/components/CandidateAvatar.tsx
// Responsive candidate avatar supporting photoAssetId streaming from Asset Service or initials fallback.

import React, { useState, useEffect } from 'react';
import { User, ShieldCheck, Loader2 } from 'lucide-react';
import { assetService } from '../services/assetService';

export type AvatarSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';

interface CandidateAvatarProps {
  photoAssetId?: string | null;
  photoUrl?: string | null;
  name?: string;
  size?: AvatarSize;
  shape?: 'circle' | 'square' | 'rounded';
  showVerifiedBadge?: boolean;
  className?: string;
  bordered?: boolean;
}

const SIZE_MAP: Record<AvatarSize, { container: string; text: string; icon: string; badge: string }> = {
  xs: { container: 'w-7 h-7', text: 'text-xs', icon: 'w-3.5 h-3.5', badge: 'w-2.5 h-2.5 -bottom-0.5 -right-0.5' },
  sm: { container: 'w-9 h-9', text: 'text-xs font-medium', icon: 'w-4 h-4', badge: 'w-3 h-3 -bottom-0.5 -right-0.5' },
  md: { container: 'w-11 h-11', text: 'text-sm font-semibold', icon: 'w-5 h-5', badge: 'w-3.5 h-3.5 -bottom-0.5 -right-0.5' },
  lg: { container: 'w-16 h-16', text: 'text-lg font-bold', icon: 'w-8 h-8', badge: 'w-4 h-4 bottom-0 right-0' },
  xl: { container: 'w-24 h-24', text: 'text-2xl font-bold', icon: 'w-12 h-12', badge: 'w-6 h-6 bottom-1 right-1' },
  '2xl': { container: 'w-32 h-32', text: 'text-3xl font-extrabold', icon: 'w-16 h-16', badge: 'w-7 h-7 bottom-1 right-1' },
};

export const CandidateAvatar: React.FC<CandidateAvatarProps> = ({
  photoAssetId,
  photoUrl: initialPhotoUrl,
  name = 'Candidate',
  size = 'md',
  shape = 'circle',
  showVerifiedBadge = false,
  className = '',
  bordered = false,
}) => {
  const [resolvedUrl, setResolvedUrl] = useState<string | null>(initialPhotoUrl || null);
  const [loading, setLoading] = useState<boolean>(Boolean(photoAssetId && !initialPhotoUrl));
  const [hasError, setHasError] = useState<boolean>(false);

  useEffect(() => {
    if (initialPhotoUrl) {
      setResolvedUrl(initialPhotoUrl);
      setLoading(false);
      setHasError(false);
      return;
    }

    if (!photoAssetId) {
      setResolvedUrl(null);
      setLoading(false);
      setHasError(false);
      return;
    }

    let isMounted = true;
    setLoading(true);
    setHasError(false);

    assetService
      .fetchAssetBlobUrl(photoAssetId)
      .then((url) => {
        if (isMounted) {
          setResolvedUrl(url);
          setLoading(false);
        }
      })
      .catch(() => {
        if (isMounted) {
          setHasError(true);
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [photoAssetId, initialPhotoUrl]);

  // Compute initials (e.g. "John Doe" -> "JD")
  const initials = name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() || '')
    .join('') || 'C';

  const shapeClass =
    shape === 'circle' ? 'rounded-full' : shape === 'square' ? 'rounded-lg' : 'rounded-2xl';
  const borderClass = bordered ? 'ring-2 ring-indigo-500/30' : '';
  const sizeConfig = SIZE_MAP[size];

  return (
    <div className={`relative inline-block select-none ${sizeConfig.container} ${className}`}>
      <div
        className={`w-full h-full ${shapeClass} ${borderClass} overflow-hidden flex items-center justify-center bg-gradient-to-br from-indigo-600 to-indigo-800 text-white shadow-sm`}
      >
        {loading ? (
          <div className="flex items-center justify-center w-full h-full bg-slate-800">
            <Loader2 className={`${sizeConfig.icon} animate-spin text-indigo-400`} />
          </div>
        ) : resolvedUrl && !hasError ? (
          <img
            src={resolvedUrl}
            alt={`${name}'s profile photo`}
            className="w-full h-full object-cover"
            onError={() => setHasError(true)}
          />
        ) : initials ? (
          <span className={`${sizeConfig.text} tracking-wider font-semibold`}>{initials}</span>
        ) : (
          <User className={sizeConfig.icon} />
        )}
      </div>

      {showVerifiedBadge && (
        <div
          className={`absolute ${sizeConfig.badge} bg-white rounded-full flex items-center justify-center shadow-md`}
          title="Verified Identity"
        >
          <ShieldCheck className="w-full h-full text-emerald-600 fill-emerald-100" />
        </div>
      )}
    </div>
  );
};
