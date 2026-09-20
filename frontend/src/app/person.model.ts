export interface Person { id: number; firstName: string; lastName: string; age: number; addressId: number | null; }
export interface PersonRequest { firstName: string; lastName: string; age: number; addressId: number | null; }
export interface PersonImportResponse { status: string; read: number; written: number; ignored: number; }
