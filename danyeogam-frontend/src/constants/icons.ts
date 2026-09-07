import {
  ArrowLeft,
  BookOpen,
  ChevronDown,
  LocateFixed,
  Map,
  MapPin,
  Navigation,
  ParkingCircle,
  Share2,
  Star,
  X,
} from "lucide-react";

/**
 * 디자인 시스템 8장: "아이콘 자체를 전통 문양처럼 만들기보다 현대적인 아이콘을 사용한다."
 * 컴포넌트에서는 lucide-react를 직접 import하지 않고 이 의미 이름을 통해 사용해서,
 * 나중에 아이콘 세트를 바꾸더라도 이 파일만 고치면 되게 한다.
 */
export const AppIcon = {
  location: MapPin,
  currentLocation: LocateFixed,
  map: Map,
  collection: BookOpen,
  parking: ParkingCircle,
  navigation: Navigation,
  favorite: Star,
  share: Share2,
  close: X,
  back: ArrowLeft,
  chevronDown: ChevronDown,
} as const;
