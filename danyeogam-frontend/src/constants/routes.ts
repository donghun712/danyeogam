export const ROUTES = {
  map: "/",
  collection: "/collection",
  attractionDetail: (id: number | string = ":attractionId") =>
    `/attractions/${id}`,
  attractionParking: (id: number | string = ":attractionId") =>
    `/attractions/${id}/parking`,
  attractionStamp: (id: number | string = ":attractionId") =>
    `/attractions/${id}/stamp`,
} as const;
