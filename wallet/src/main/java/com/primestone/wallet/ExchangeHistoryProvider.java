package com.primestone.wallet;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinID;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.exceptions.AddressMalformedException;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftTxStatus;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftTxStatus.Status;
import com.coinomi.core.wallet.AbstractAddress;
import java.io.Serializable;
import java.util.List;

public class ExchangeHistoryProvider extends ContentProvider {
    private Helper helper;

    public static class ExchangeEntry implements Serializable {
        public final AbstractAddress depositAddress;
        public final Value depositAmount;
        public final String depositTransactionId;
        public final int status;
        public final AbstractAddress withdrawAddress;
        public final Value withdrawAmount;
        public final String withdrawTransactionId;

        public ExchangeEntry(int status, AbstractAddress depositAddress, Value depositAmount, String depositTransactionId, AbstractAddress withdrawAddress, Value withdrawAmount, String withdrawTransactionId) {
            this.status = status;
            this.depositAddress = (AbstractAddress) Preconditions.checkNotNull(depositAddress);
            this.depositAmount = (Value) Preconditions.checkNotNull(depositAmount);
            this.depositTransactionId = (String) Preconditions.checkNotNull(depositTransactionId);
            this.withdrawAddress = withdrawAddress;
            this.withdrawAmount = withdrawAmount;
            this.withdrawTransactionId = withdrawTransactionId;
        }

        public ExchangeEntry(AbstractAddress depositAddress, Value depositAmount, String depositTxId) {
            this(0, depositAddress, depositAmount, depositTxId, null, null, null);
        }

        public ExchangeEntry(ExchangeEntry initialEntry, ShapeShiftTxStatus txStatus) {
            this.status = convertStatus(txStatus.status);
            this.depositAddress = (AbstractAddress) Preconditions.checkNotNull(txStatus.address == null ? initialEntry.depositAddress : txStatus.address);
            this.depositAmount = (Value) Preconditions.checkNotNull(txStatus.incomingValue == null ? initialEntry.depositAmount : txStatus.incomingValue);
            this.depositTransactionId = (String) Preconditions.checkNotNull(initialEntry.depositTransactionId);
            this.withdrawAddress = txStatus.withdraw;
            this.withdrawAmount = txStatus.outgoingValue;
            this.withdrawTransactionId = txStatus.transactionId;
        }

        public ContentValues getContentValues() {
            ContentValues values = new ContentValues();
            values.put("status", Integer.valueOf(this.status));
            values.put("deposit_address", this.depositAddress.toString());
            values.put("deposit_coin_id", this.depositAddress.getType().getId());
            values.put("deposit_amount_unit", Long.valueOf(this.depositAmount.value));
            values.put("deposit_txid", this.depositTransactionId);
            if (this.withdrawAddress != null) {
                values.put("withdraw_address", this.withdrawAddress.toString());
            }
            if (this.withdrawAddress != null) {
                values.put("withdraw_coin_id", this.withdrawAddress.getType().getId());
            }
            if (this.withdrawAmount != null) {
                values.put("withdraw_amount_unit", Long.valueOf(this.withdrawAmount.value));
            }
            if (this.withdrawTransactionId != null) {
                values.put("withdraw_txid", this.withdrawTransactionId);
            }
            return values;
        }

        public static int convertStatus(Status shapeShiftStatus) {
            switch (shapeShiftStatus) {
                case NO_DEPOSITS:
                    return 0;
                case RECEIVED:
                    return 1;
                case COMPLETE:
                    return 2;
                case FAILED:
                    return -1;
                default:
                    return -2;
            }
        }
    }

    private static class Helper extends SQLiteOpenHelper {
        public Helper(Context context) {
            super(context, "exchange_history", null, 2);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE exchange_history (_id INTEGER PRIMARY KEY AUTOINCREMENT, status INTEGER NOT NULL, deposit_address TEXT NOT NULL, deposit_coin_id TEXT NOT NULL, deposit_amount_unit INTEGER NOT NULL, deposit_txid TEXT NOT NULL, withdraw_address TEXT NULL, withdraw_coin_id TEXT NULL, withdraw_amount_unit INTEGER NULL, withdraw_txid TEXT NULL);");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.beginTransaction();
            int v = oldVersion;
            while (v < newVersion) {
                try {
                    upgrade(db, v);
                    v++;
                } catch (Throwable th) {
                    db.endTransaction();
                }
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        }

        private void upgrade(SQLiteDatabase db, int oldVersion) {
            if (oldVersion == 1) {
                db.execSQL(renameCoinId("deposit_coin_id", "darkcoin.main", "dash.main"));
                db.execSQL(renameCoinId("withdraw_coin_id", "darkcoin.main", "dash.main"));
                return;
            }
            throw new UnsupportedOperationException("old=" + oldVersion);
        }

        private String renameCoinId(String fieldName, String from, String to) {
            return "UPDATE exchange_history SET " + fieldName + " = replace(" + fieldName + ", \"" + from + "\", \"" + to + "\") " + "WHERE " + fieldName + " == \"" + from + "\"";
        }
    }

    public static Uri contentUri(String packageName, AbstractAddress deposit) {
        return Uri.parse("content://" + packageName + '.' + "exchange_history").buildUpon().appendPath(deposit.getType().getId()).appendPath(deposit.toString()).build();
    }

    public static Uri contentUri(String packageName) {
        return Uri.parse("content://" + packageName + '.' + "exchange_history");
    }

    public static ExchangeEntry getExchangeEntry(Cursor cursor) {
        int status = getStatus(cursor);
        CoinType depositType = CoinID.typeFromId(cursor.getString(cursor.getColumnIndexOrThrow("deposit_coin_id")));
        try {
            AbstractAddress withdrawAddress;
            Value withdrawAmount;
            String withdrawTxId;
            AbstractAddress depositAddress = depositType.newAddress(cursor.getString(cursor.getColumnIndexOrThrow("deposit_address")));
            Value depositAmount = depositType.value(cursor.getLong(cursor.getColumnIndexOrThrow("deposit_amount_unit")));
            String depositTxId = cursor.getString(cursor.getColumnIndexOrThrow("deposit_txid"));
            try {
                CoinType withdrawType = CoinID.typeFromId(cursor.getString(cursor.getColumnIndexOrThrow("withdraw_coin_id")));
                withdrawAddress = withdrawType.newAddress(cursor.getString(cursor.getColumnIndexOrThrow("withdraw_address")));
                withdrawAmount = withdrawType.value(cursor.getLong(cursor.getColumnIndexOrThrow("withdraw_amount_unit")));
                withdrawTxId = cursor.getString(cursor.getColumnIndexOrThrow("withdraw_txid"));
            } catch (Exception e) {
                withdrawAddress = null;
                withdrawAmount = null;
                withdrawTxId = null;
            }
            return new ExchangeEntry(status, depositAddress, depositAmount, depositTxId, withdrawAddress, withdrawAmount, withdrawTxId);
        } catch (AddressMalformedException e2) {
            throw new RuntimeException(e2);
        }
    }

    public static int getStatus(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndexOrThrow("status"));
    }

    public boolean onCreate() {
        this.helper = new Helper(getContext());
        return true;
    }

    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    private AbstractAddress getDepositAddress(Uri uri) {
        List<String> pathSegments = getPathSegments(uri);
        try {
            return CoinID.typeFromId((String) pathSegments.get(0)).newAddress((String) pathSegments.get(1));
        } catch (AddressMalformedException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private List<String> getPathSegments(Uri uri) {
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() == 2) {
            return pathSegments;
        }
        throw new IllegalArgumentException(uri.toString());
    }

    public Uri insert(Uri uri, ContentValues values) {
        AbstractAddress address = getDepositAddress(uri);
        values.put("deposit_coin_id", address.getType().getId());
        values.put("deposit_address", address.toString());
        Uri rowUri = contentUri(getContext().getPackageName(), address).buildUpon().appendPath(Long.toString(this.helper.getWritableDatabase().insertOrThrow("exchange_history", null, values))).build();
        getContext().getContentResolver().notifyChange(rowUri, null);
        return rowUri;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        AbstractAddress address = getDepositAddress(uri);
        values.put("deposit_coin_id", address.getType().getId());
        values.put("deposit_address", address.toString());
        int count = this.helper.getWritableDatabase().update("exchange_history", values, "deposit_coin_id=? AND deposit_address=?", new String[]{address.getType().getId(), address.toString()});
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        AbstractAddress address = getDepositAddress(uri);
        int count = this.helper.getWritableDatabase().delete("exchange_history", "deposit_coin_id=? AND deposit_address=?", new String[]{address.getType().getId(), address.toString()});
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("exchange_history");
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() > 2) {
            throw new IllegalArgumentException(uri.toString());
        }
        if (pathSegments.size() == 2) {
            AbstractAddress address = getDepositAddress(uri);
            qb.appendWhere("deposit_coin_id=");
            qb.appendWhereEscapeString(address.getType().getId());
            qb.appendWhere(" AND deposit_address=");
            qb.appendWhereEscapeString(address.toString());
        }
        Cursor cursor = qb.query(this.helper.getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }
}
